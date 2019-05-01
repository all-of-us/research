"""Produce a list of API endpoints that are dialed by AoU endpoints.

This program requires two command-line parameters:

    1. The path to the root of an AoU code repository
    2. The path to a file containing a call graph of the AoU Java API code

The call graph file is formatted as described here: https://github.com/gousiosg/java-callgraph

To generate the call graph, do the following:

```
git clone https://github.com/gousiosg/java-callgraph.git
cd path/to/java-callgraph
mvn install # install 'maven' before running this command
java -jar target/javacg-0.1-SNAPSHOT-static.jar path-to-aou-repo/{aou-utils/swagger-codegen-cli.jar,common-api/build/libs/common-api-0.1.0.jar} >path/to/callgraph.txt
```

Then, to produce the list of endpoints, run `python3 api.py path/to/aou-repo path/to/callgraph.txt`. The endpoints are output in CSV.

"""
import collections
import os
import pprint
import re
import sys
import yaml
import operator

def capitalized(s):
    """Return a string with its first character capitalized."""
    if not s:
        return s
    return s[0].upper() + s[1:]

class Method:
    def __init__(self, klass, name):
        self.klass = klass
        self.name = name

    def __hash__(self):
        return hash(self.klass) * 31 + hash(self.name)

    def __eq__(self, other):
        return (isinstance(other, Method) or isinstance(other, Endpoint)) and\
            self.klass == other.klass and \
            self.name == other.name

    def __repr__(self):
        return "Method({0}, {1})".format(repr(self.klass), repr(self.name))
    
class Endpoint:
    """A REST API endpoint."""
    def __init__(self, verb, path, klass, method, api=None):
        self.verb = verb
        self.path = path
        
        # Endpoints are Methods, so add Method fields.
        self.klass = klass
        self.name = method

        self.api = None

    def __eq__(self, other):
        if isinstance(other, Method):
            return self.klass == other.klass and \
                self.name == other.name
        if isinstance(other, Endpoint):
            return self.verb == other.verb and \
                self.path == other.path and \
                self.klass == other.klass and \
                self.name == other.name and \
                self.api == self.api
        return False

    def __repr__(self):
        return "Endpoint({0}, {1}, {2}, {3})".format(
            repr(self.verb),
            repr(self.path),
            repr(self.klass),
            repr(self.name),
            repr(self.api)
        )

    def __hash__(self):
        return hash(self.klass) * 31 + hash(self.name)

def edges_from_endpoint(endpoint):
    """Produce edges that connect the endpoint method to the other methods generated by Swagger."""
    return list(map(lambda method: Edge(endpoint, method, order=1),
                    methods_from_endpoint(endpoint)))

def methods_from_endpoint(endpoint):
    """Produce a list of classes generated by Swagger."""
    methods = []
    class_prefix = endpoint.klass[:endpoint.klass.rfind("Api")]
    for suffix in ('ApiDelegate', 'Controller', 'ApiController'):
        klass = "{0}{1}".format(class_prefix, suffix)
        name = endpoint.name
        method = Method(klass, name)
        assert method != Method('org.pmiops.workbench.firecloud.api.StatusApi', 'status')
        methods.append(method)
    return methods

def read_endpoints_from_swagger_yaml(path, package):
    """Produce endpoints from Swagger yaml file."""
    endpoints = []
    with open(path) as fp:
        root = yaml.safe_load(fp)
        for path, attr in root['paths'].items():
            for verb, vattr in attr.items():
                if 'tags' not in vattr or not vattr['tags']:
                    # Can't generate endpoints without a class name
                    continue
                klass = '{0}.{1}Api'.format(package, capitalized(vattr['tags'][0]))
                method = vattr['operationId']
                ep = Endpoint(
                    verb,
                    path,
                    klass,
                    method
                )
                endpoints.append(ep)
    return endpoints

class Edge:
    def __init__(self, origin, destination, order=0):
        self.origin = origin
        self.destination = destination
        self.order = order

    def __eq__(self, other):
        return isinstance(other, Edge) and \
            self.origin == other.origin and \
            self.destination == other.destination and \
            self.order == other.order

    def __lt__(self, other):
        return self.order < other.order

    def __hash__(self):
        h = hash(self.origin)
        h = h*31 + hash(self.destination)
        h = h*31 + hash(self.order)
        return h

    def __repr__(self):
        return "Edge({0}, {1}, order={2})".format(
            self.origin,
            self.destination,
            self.order
        )

def callgraph_edges_from_file(fp, endpoints):
    """Produce edges from call graph."""
    pat = re.compile(r'M:([^:]+):([^(]+)\([^)]*\) \(\w\)([^:]+):([^(]+)')
    lambdapat = re.compile(r'lambda\$([^$]+)\$')
    edges = []
    order = 0
    prev_src_class = None
    prev_src_method = None
    for line in fp:
        m = pat.match(line)
        if not m:
            continue
        src_class = m[1]
        src_method = m[2]
        order += -order if (src_class != prev_src_class or src_method != prev_src_method) else 1
        prev_src_class = src_class
        prev_src_method = src_method
        dst_class = m[3]
        dst_method = m[4]
        caller = Method(src_class, src_method)
        caller = endpoints.get(caller, caller)
        callee = Method(dst_class, dst_method)
        callee = endpoints.get(callee, callee)
        edges.append(Edge(caller, callee, order=order))
        if src_class.endswith("Impl"):
            # Also produce the edge from the base class to the
            # implementing class. Example: The node ("FooImpl",
            # "boop") produces the extra edge (("Foo", "boop"),
            # ("FooImpl", "boop")).
            caller = Method(src_class[:-4], src_method)
            caller = endpoints.get(caller, caller)
            callee = Method(src_class, src_method)
            callee = endpoints.get(callee, callee)
            assert caller != Method('org.pmiops.workbench.firecloud.api.StatusApi', 'status')
            assert callee != Method('org.pmiops.workbench.firecloud.api.StatusApi', 'status')
            edges.append(Edge(caller, callee))
        if src_method.startswith("lambda$"):
            m = lambdapat.match(src_method)
            if not m:
                continue
            # Also produce the edge from a method that makes an
            # anonymous class, to the method of the anonymous class.
            # Example: The node ("Foo", "lambda$boop$1") produces the
            # extra edge (("Foo", "boop"), ("Foo", "lambda$boop$1")).
            caller = Method(src_class, m[1])
            caller = endpoints.get(caller, caller)
            callee = Method(src_class, src_method)
            callee = endpoints.get(callee, callee)
            assert caller != Method('org.pmiops.workbench.firecloud.api.StatusApi', 'status')
            assert callee != Method('org.pmiops.workbench.firecloud.api.StatusApi', 'status')
            edges.append(Edge(caller, callee))
    return edges

def graph_add_edge(graph, edge):
    """Add an edge to a graph."""
    origin = edge.origin
    if origin not in graph:
        graph[origin] = set()
    graph[origin].add(edge)

def make_graph(*edge_sources):
    """Construct a graph from the given edge sources."""
    graph = {}
    for edges in edge_sources:
        for edge in edges:
            graph_add_edge(graph, edge)
    return graph

def graph_search(graph, origin, destination):
    """Uses breadth-first search to find a path between src and dst."""
    if origin == destination:
        return []
    frontier = collections.deque([Path(origin)])
    frontier_nodes = set()
    frontier_nodes.add(origin)
    visited = set()
    while frontier:
        path = frontier.popleft()
        node = path.method
        frontier_nodes.remove(node)
        visited.add(node)
        neighbor_edges = graph.get(node, [])
        for neighbor_edge in neighbor_edges:
            neighbor = neighbor_edge.destination
            newpath = path.to(neighbor, neighbor_edge)
            if (neighbor not in visited) and (neighbor not in frontier_nodes):
                if neighbor == destination:
                    return newpath
                frontier.append(newpath)
                frontier_nodes.add(neighbor)
    return None

def flatmap(fn, seq):
    """
    Map the fn to each element of seq and append the results of the
    sublists to a resulting list.
    """
    result = []
    for lst in map(fn, seq):
        for elt in lst:
            result.append(elt)
    return result

def cross_product(seq1, seq2):
    for a in seq1:
        for b in seq2:
            yield (a, b)

def group_by(keyfn, seq):
    first_elt = True
    key = None
    group = []
    for elt in seq:
        if first_elt:
            group.append(elt)
            first_elt = False
            key = keyfn(elt)
        elif keyfn(elt) == key:
            group.append(elt)
        else:
            yield group
            group = [elt]
            key = keyfn(elt)
    if group:
        yield group

class API:
    """A Swagger API."""
    def __init__(self, name, path, package):
        self.name = name
        self.path = path
        self.package = package

    def __repr__(self):
        return "API({0}, {1}, {2})".format(
            repr(self.name),
            repr(self.path),
            repr(self.package)
        )

def read_callgraph_edges(path, endpoints):
    """Read the callgraph edges from a file."""
    with open(path) as fp:
        return list(callgraph_edges_from_file(fp, endpoints))

class CallTree:
    def __init__(self, method=None, order=0):
        self.method = method
        self.order = order
        self.children = []

    def __repr__(self):
        return "CallTree(method={0}, order={1} children={2})".format(
            repr(self.method), repr(self.order), repr(self.children))

    def insert(self, *paths):
        for path in paths:
            parent = self
            subtrees = map(
                lambda p: CallTree(method=p[0], order=p[1]),
                zip(path.methods(), [0] + [edge.order for edge in path.edges()])
            )
            for subtree in subtrees:
                if subtree in parent.children:
                    parent = parent.children[parent.children.index(subtree)]
                else:
                    parent.children = sorted(parent.children + [subtree])
                    parent = subtree
        return self

    def traverse(self, fn):
        for child in self.children:
            self._traverse_rec(fn, child)

    def _traverse_rec(self, fn, root, depth=0):
        fn(root.method, depth, not root.children)
        for child in root.children:
            self._traverse_rec(fn, child, depth+1)

    def __lt__(self, other):
        assert type(other) == type(self)
        return self.order < other.order

    def __eq__(self, other):
        assert type(other) == type(self)
        return self.method == other.method and self.order == other.order

class Path:
    def __init__(self, method, parent=None, edge=None):
        self.method = method
        self.parent = parent
        self.edge = edge

    def methods(self):
        node = self
        L = []
        while node and node.method:
            L.append(node.method)
            node = node.parent
        return list(reversed(L))

    def edges(self):
        node = self
        L = []
        while node and node.edge:
            L.append(node.edge)
            node = node.parent
        return list(reversed(L))

    def to(self, method, edge):
        return Path(method, parent=self, edge=edge)

def tree_print(method, depth, isendpoint):
    print("{0}{1}".format('\t' * depth, method))

def read_endpoints_from_api(api):
    endpoints = read_endpoints_from_swagger_yaml(api.path, api.package)
    for ep in endpoints:
        ep.api = api
    return endpoints
    
def main(argv):
    """Program entry point."""
    assert len(argv) > 0
    
    EXIT_SUCCESS = 0
    EXIT_FAILURE = 1
    if len(argv) != 3:
        sys.stderr.write("Usage: python {0} project-root callgraph-file\n".format(argv[0]))
        return EXIT_FAILURE
    project_root = argv[1]
    callgraph_path = argv[2]
    apis = [
        API("Workbench", os.path.join(project_root, "api/src/main/resources/workbench.yaml"), "org.pmiops.workbench.api"),
        API("Terra", os.path.join(project_root, "api/src/main/resources/fireCloud.yaml"), "org.pmiops.workbench.firecloud.api"),
        API("Notebooks", os.path.join(project_root, "api/src/main/resources/notebooks.yaml"), "org.pmiops.workbench.notebooks.api"),
        API("JIRA", os.path.join(project_root, "api/src/main/resources/jira.yaml"), "org.pmiops.workbench.jira.api"),
        API("Mandrill", os.path.join(project_root, "api/src/main/resources/mandrill_api.yaml"), "org.pmiops.workbench.mandrill.api"),
        API("Moodle", os.path.join(project_root, "api/src/main/resources/moodle.yaml"), "org.pmiops.workbench.moodle.api"),
    ]
    def make_endpoint_printer(apis, origin):
        state = {"print_origin": True}
        def print_endpoint(method, depth, isendpoint):
            if not isendpoint:
                return
            if state["print_origin"]:
                print(origin.path, end='')
                state["print_origin"] = False
            index = apis.index(method.api)
            print("{0}{1}".format(',' * index, method.path))
        return print_endpoint

    origin_endpoints = read_endpoints_from_api(apis[0])
    target_endpoints = flatmap(read_endpoints_from_api, apis[1:])
    api_edges = flatmap(edges_from_endpoint, origin_endpoints + target_endpoints)
    callgraph_edges = read_callgraph_edges(callgraph_path, {k: k for k in (origin_endpoints + target_endpoints)})
    graph = make_graph(callgraph_edges, api_edges)

    print(','.join(map(lambda api: api.name, apis)))
    endpoint_paths = group_by(
        lambda path: path.methods()[0], # Group by starting API endpoint (e.g. /v1/me)
        filter(
            None,
            map(lambda edge: graph_search(graph, *edge), cross_product(origin_endpoints, target_endpoints))
        )
    )
    for group in endpoint_paths:
        print_endpoint = make_endpoint_printer(apis, group[0].methods()[0])
        CallTree().insert(*group).traverse(print_endpoint)
    return EXIT_SUCCESS

if __name__ == "__main__":
    sys.exit(main(sys.argv))
