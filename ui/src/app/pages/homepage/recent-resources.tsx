import * as fp from 'lodash/fp';
import * as React from 'react';

import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {CSSProperties, useEffect, useState} from 'react';

import {Clickable} from 'app/components/buttons';
import {SmallHeader} from 'app/components/headers';
import {renderResourceCard} from 'app/components/render-resource-card';
import {ResourceNavigation, StyledResourceType} from 'app/components/resource-card';
import {SpinnerOverlay} from 'app/components/spinners';
import {userMetricsApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {formatWorkspaceResourceDisplayDate, getCdrVersion, reactStyles, withCdrVersions} from 'app/utils';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {getDisplayName} from 'app/utils/resources';
import {CdrVersionListResponse, Workspace, WorkspaceResource, WorkspaceResourceResponse} from 'generated/fetch';

const styles = reactStyles({
  column: {
    textAlign: 'left',
  },
  typeColumn: {
    textAlign: 'left',
    width: '130px',
  },
  menu: {
    width: '30px',
  },
  navigation: {
    fontFamily: 'Montserrat',
    fontSize: '14px',
    letterSpacing: 0,
    lineHeight: '22px',
  }
});

const WorkspaceNavigation = (props: {workspace: Workspace, style?: CSSProperties}) => {
  const {workspace: {name, namespace, id}, style} = props;
  const url = `/workspaces/${namespace}/${id}/data`;

  return <Clickable>
    <a data-test-id='workspace-navigation'
       style={style}
       href={url}
       onClick={e => navigateAndPreventDefaultIfNoKeysPressed(e, url)}>
      {name}
    </a>
  </Clickable>;
};

interface TableData {
  menu: JSX.Element;
  resourceType: JSX.Element;
  resourceName: JSX.Element;
  workspaceName: JSX.Element;
  formattedLastModified: string;
  cdrVersionName: string;
}

const RecentResources = fp.flow(withCdrVersions())((props: {cdrVersionListResponse: CdrVersionListResponse}) => {
  const [loading, setLoading] = useState(true);
  const [resources, setResources] = useState<WorkspaceResourceResponse>();
  const [wsMap, setWorkspaceMap] = useState<Map<string, Workspace>>();
  const [tableData, setTableData] = useState<TableData[]>();

  const loadResources = () => {
    setLoading(true);
    return userMetricsApi().getUserRecentResources()
      .then(setResources)
      .then(() => setLoading(false));
  };

  useEffect(() => {
    loadResources();
  }, []);

  useEffect(() => {
    workspacesApi().getWorkspaces().then(response => {
      const workspaces = response.items.map(r => [r.workspace.id, r.workspace] as [string, Workspace]);
      setWorkspaceMap(new Map(workspaces));
    });
  }, []);

  const renderResourceMenu = (resource: WorkspaceResource) => {
    return renderResourceCard({
      resource,
      menuOnly: true,
      existingNameList: [],   // TODO existing bug RW-5847: does not populate names for rename modal
      onUpdate: loadResources});
  };

  useEffect(() => {
    const getWorkspace = (r: WorkspaceResource) => {
      return wsMap.get(r.workspaceFirecloudName);
    };

    const getCdrVersionName = (r: WorkspaceResource) => {
      const {cdrVersionListResponse} = props;
      return getCdrVersion(getWorkspace(r), cdrVersionListResponse).name;
    };

    if (resources && wsMap) {
      setTableData(resources.map(r => {
        return {
          menu: renderResourceMenu(r),
          resourceType: <ResourceNavigation resource={r}><StyledResourceType resource={r}/></ResourceNavigation>,
          resourceName: <ResourceNavigation resource={r} style={styles.navigation}>{getDisplayName(r)}</ResourceNavigation>,
          workspaceName: <WorkspaceNavigation workspace={getWorkspace(r)} style={styles.navigation}/>,
          formattedLastModified: formatWorkspaceResourceDisplayDate(r.modifiedTime),
          cdrVersionName: getCdrVersionName(r),
        };
      }));
    }
  }, [resources, wsMap]);

  return (resources && wsMap && !loading) ? <React.Fragment>
    <SmallHeader>Recently Accessed Items</SmallHeader>
      <div data-test-id='recent-resources-table'><DataTable
          value={tableData}
          scrollable={true}
          paginator={true}
          paginatorTemplate='CurrentPageReport'
          currentPageReportTemplate='Showing {totalRecords} most recent items'>
        <Column field='menu' style={styles.menu}/>
        <Column field='resourceType' header='Item type' style={styles.typeColumn}/>
        <Column field='resourceName' header='Name' style={styles.column}/>
        <Column field='workspaceName' header='Workspace name' style={styles.column}/>
        <Column field='formattedLastModified' header='Last changed' style={styles.column}/>
        <Column field='cdrVersionName' header='Dataset' style={styles.column}/>
      </DataTable></div>
  </React.Fragment> : <SpinnerOverlay/>;
});

export {
  RecentResources
};
