import {select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {DomainType, TreeSubType, TreeType} from 'generated';
import {Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {DOMAIN_TYPES, PROGRAM_TYPES} from '../constant';
import {
  activeCriteriaSubtype,
  activeCriteriaTreeType,
  activeCriteriaType,
  activeItem,
  activeParameterList,
  CohortSearchActions,
  nodeAttributes,
  previewStatus,
  subtreeSelected,
  wizardOpen,
} from '../redux';
import {stripHtml, subtypeToTitle, typeToTitle} from '../utils';


@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: [
    './modal.component.css',
    '../../styles/buttons.css',
  ]
})
export class ModalComponent implements OnInit, OnDestroy {
  @select(wizardOpen) open$: Observable<boolean>;
  @select(activeCriteriaSubtype) criteriaSubtype$: Observable<any>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;
  @select(activeCriteriaTreeType) isFullTree$: Observable<boolean>;
  @select(activeItem) item$: Observable<any>;
  @select(activeParameterList) selection$: Observable<any>;
  @select(nodeAttributes) attributes$: Observable<any>;
  @select(subtreeSelected) scrollTo$: Observable<any>;
  @select(previewStatus) preview$;

  readonly domainType = DomainType;
  readonly treeType = TreeType;
  ctype: string;
  subtype: string;
  itemType: string;
  fullTree: boolean;
  subscription: Subscription;
  attributesNode: Map<any, any> = Map();
  selections = {};
  objectKey = Object.keys;
  open = false;
  noSelection = true;
  title = '';
  mode: 'tree' | 'modifiers' | 'attributes' | 'snomed' = 'tree'; // default to criteria tree
  demoItemsType: string;
  demoParam: string;
  count = 0;
  originalNode: any;
  disableCursor = false;
  preview = Map();

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.open$
      .filter(open => !!open)
      .subscribe(_ => {
        // reset to default each time the modal is opened
        this.mode = 'tree';
        this.open = true;
      });

    this.subscription.add(this.preview$.subscribe(prev => this.preview = prev));

    this.subscription.add(this.criteriaType$
      .filter(ctype => !!ctype)
      .subscribe(ctype => {
        this.ctype = ctype;
      })
    );

    this.subscription.add(this.isFullTree$.subscribe(fullTree => this.fullTree = fullTree));
    this.subscription.add(this.selection$
      .subscribe(selections => {
        this.selections = {};
        this.noSelection = selections.size === 0;
        selections.forEach(selection => {
          this.addSelectionToGroup(selection);
        });
      })
    );
      this.subscription.add(this.selection$
          .map(sel => sel.size === 0)
          .subscribe(sel => this.noSelection = sel)
      );
    this.subscription.add(this.attributes$
      .subscribe(node => {
        this.attributesNode = node;
        if (node.size === 0) {
          this.mode = 'tree';
        } else {
          this.mode = 'attributes';
        }
      })
    );

    this.subscription.add(this.scrollTo$
      .filter(nodeIds => !!nodeIds)
      .subscribe(nodeIds => {
        this.setScroll(nodeIds[0]);
      })
    );

    this.subscription.add(this.item$.subscribe(item => {
      this.itemType = item.get('type');
      this.title = 'Codes';
      for (const crit of DOMAIN_TYPES) {
        const regex = new RegExp(`.*${crit.type}.*`, 'i');
        if (regex.test(this.itemType)) {
          this.title = crit.name;
        }
      }
      for (const crit of PROGRAM_TYPES) {
        const regex = new RegExp(`.*${crit.type}.*`, 'i');
        if (regex.test(this.itemType)) {
          this.title = crit.name;
        }
      }
    }));

    this.subscription.add(this.criteriaSubtype$
      .subscribe(subtype => {
        this.subtype = subtype;
      })
    );
    this.originalNode = this.rootNode;
  }
  addSelectionToGroup(selection: any) {
    const key = selection.get('type') === TreeType[TreeType.DEMO]
      ? selection.get('subtype') : selection.get('type');
    if (this.selections[key] && !this.selections[key].includes(selection)) {
      this.selections[key].push(selection);
    } else {
      this.selections[key] = [selection];
    }
  }
  setScroll(nodeId: string) {
    let node: any;
    this.disableCursor = true;
    Observable.interval(100)
      .takeWhile((val, index) => !node && index < 30)
      .subscribe(i => {
        node = document.getElementById('node' + nodeId.toString());
        if (node) {
          setTimeout(() => {
            node.scrollIntoView({behavior: 'smooth'});
            this.disableCursor = false;
          }, 200);
        } else if (i === 29) {
          this.disableCursor = false;
        }
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  cancel() {
    this.selections = {};
    this.open = false;
    this.actions.cancelWizard(this.ctype, 0);
  }

  back() {
    if (this.attributesNode.size > 0) {
      this.actions.hideAttributesPage();
    }
    if (this.mode === 'snomed') {
      this.setMode('tree');
    }
  }

  finish() {
    this.selections = {};
    this.open = false;
    this.actions.finishWizard();
  }

  /* Used to bootstrap the criteria tree */
  get rootNode() {
    return Map({
      type: this.ctype,
      subtype: this.subtype,
      fullTree: this.fullTree,
      id: 0,    // root parent ID is always 0
    });
  }

  get snomedNode() {
    return Map({
      type: TreeType.SNOMED,
      subtype: this.subtype === TreeSubType[TreeSubType.CM]
        ? TreeSubType.CM : TreeSubType.PCS,
      fullTree: this.fullTree,
      id: 0,    // root parent ID is always 0
    });
  }

  get selectionTitle() {
    const _type = [
      TreeType[TreeType.CONDITION],
      TreeType[TreeType.PROCEDURE]
    ].includes(this.itemType)
      ? this.itemType : this.ctype;
    const title = typeToTitle(_type);
    return title
      ? `Add Selected ${title} Criteria to Cohort`
      : 'No Selection';
  }

  get attributeTitle() {
    return this.ctype === TreeType[TreeType.PM]
      ? stripHtml(this.attributesNode.get('name'))
      : typeToTitle(this.ctype) + ' Detail';
  }

  get showModifiers() {
    return this.itemType !== TreeType[TreeType.PM] &&
          this.itemType !== TreeType[TreeType.DEMO] &&
          this.itemType !== TreeType[TreeType.PPI];
  }

  get showHeader() {
    return this.itemType === TreeType[TreeType.CONDITION]
    || this.itemType === TreeType[TreeType.PROCEDURE]
    || this.itemType === TreeType[TreeType.DEMO];
  }

  get showSnomed() {
    return this.itemType === TreeType[TreeType.CONDITION]
    || this.itemType === TreeType[TreeType.PROCEDURE];
  }

  setMode(mode: any) {
    if (mode === 'snomed') {
      this.originalNode = Map({
        type: this.ctype,
        subtype: this.subtype,
        fullTree: this.fullTree,
        id: 0,
      });
    }
    const node = mode === 'tree' ? this.originalNode : this.snomedNode;
    const criteriaType = node.get('type');
    const criteriaSubtype = node.get('subtype');
    const context = {criteriaType, criteriaSubtype};
    this.actions.setWizardContext(context);
    this.mode = mode;
  }

  get altTab() {
    return this.attributesNode.size > 0;
  }

  selectionHeader(_type: string) {
    return this.itemType === TreeType[TreeType.DEMO] ? subtypeToTitle(_type) : typeToTitle(_type);
  }

  getDemoParams(e) {
    if (e) {
      this.demoItemsType = e.type;
      this.demoParam = e.paramId;
    }
  }

   get disableFlag() {
       return !this.preview.get('requesting') && this.preview.get('count') >= 0;
   }
}

