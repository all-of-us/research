import {Component, Input} from '@angular/core';
import {ReviewDomainChartsComponent} from 'app/cohort-review/review-domain-charts/review-domain-charts';
import {vocabOptions} from 'app/cohort-review/review-state.service';
import {css} from 'app/cohort-review/review-utils/primeReactCss.utils';
import {TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {DomainType, PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {OverlayPanel} from 'primereact/overlaypanel';
import {TabPanel, TabView} from 'primereact/tabview';
import * as React from 'react';


const styles = reactStyles({
  container: {
    position: 'relative',
    minHeight: '15rem'
  },
  table: {
    fontSize: '12px',
  },
  tableBody: {
    textAlign: 'left',
    lineHeight: '0.6rem'
  },
  columnHeader: {
    background: '#f4f4f4',
    color: '#262262',
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.5rem 0.5rem 0.3rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
  },
  graphColumnBody: {
    background: '#ffffff',
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem',
    width: '2rem',
  },

  filterIcon: {
    color: '#0086C1',
    fontSize: '0.5rem',
    float: 'right'
  },
  sortIcon: {
    color: '#2691D0',
    fontSize: '0.4rem'
  },
  overlayHeader: {
    padding: '0.3rem',
  },
  caretIcon: {
    fontSize: '0.6rem',
    paddingLeft: '0.4rem',
    color: '#0086C1',
    cursor: 'pointer',
  },
  filterBorder: {
    paddingTop: '0.5rem',
    borderTop: '1px solid #ccc',
  },
  graphStyle: {
    borderLeft: 'none',
    width: '2rem',
  },
  headerStyle: {
    color: '#2691D0',
    fontSize: '14px',
    fontWeight: 'bold',
    width: '20rem',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    margin: 'auto',
    paddingTop: '0.5rem',
    textAlign: 'center',
  },
  codeSearch: {
    width: '85%',
    borderRadius: '4px',
    backgroundColor: '#dae6ed',
    marginLeft: '5px'
  }
});
const rows = 25;
const domains = [
  DomainType.CONDITION,
  DomainType.PROCEDURE,
  DomainType.DRUG,
  DomainType.OBSERVATION,
  DomainType.PHYSICALMEASURE,
  DomainType.LAB,
  DomainType.VITAL,
  DomainType.SURVEY,
];

export interface DetailTabTableProps {
  tabName: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
  workspace: WorkspaceData;
  filterState: any;
  getFilteredData: Function;
  updateState: number;
}

export interface DetailTabTableState {
  data: Array<any>;
  filteredData: Array<any>;
  loading: boolean;
  start: number;
  sortField: string;
  sortOrder: number;
  expandedRows: Array<any>;
  codeResults: Array<any>;
}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<DetailTabTableProps, DetailTabTableState> {
    codeInput: Function;
    constructor(props: DetailTabTableProps) {
      super(props);
      this.state = {
        data: null,
        filteredData: null,
        loading: true,
        start: 0,
        sortField: null,
        sortOrder: 1,
        expandedRows: [],
        codeResults: [],
      };
      this.codeInput = fp.debounce(300, (e) => this.filterCodes(e));
    }

    componentDidMount() {
      this.getParticipantData();
    }

    componentDidUpdate(prevProps: any) {
      if (prevProps.participantId !== this.props.participantId) {
        this.setState({
          data: null,
          filteredData: null,
          loading: true,
        });
        this.getParticipantData();
      } else if (prevProps.updateState !== this.props.updateState) {
        this.filterData();
      }
    }

    getParticipantData() {
      try {
        const {cdrVersionId, id, namespace} = this.props.workspace;
        const {cid} = urlParamsStore.getValue();
        const pageFilterRequest = {
          page: 0,
          pageSize: 10000,
          sortOrder: SortOrder.Asc,
          sortColumn: this.props.columns[0].name,
          pageFilterType: this.props.filterType,
          domain: this.props.domain,
        } as PageFilterRequest;

        cohortReviewApi().getParticipantData(
          namespace,
          id,
          +cid,
          +cdrVersionId,
          this.props.participantId,
          pageFilterRequest
        ).then(response => {
          response.items.forEach(item => {
            if (this.props.domain === DomainType[DomainType.VITAL]
              || this.props.domain === DomainType[DomainType.LAB]) {
              item['itemTime'] = moment(item.itemDate, 'YYYY-MM-DD HH:mm Z').format('hh:mm a z');
            }
            item.itemDate = moment(item.itemDate).format('YYYY-MM-DD');
          });
          this.setState({
            data: response.items,
            loading: false,
          });
          this.filterData();
        });
      } catch (error) {
        console.log(error);
      }
    }

    onSort = (event: any) => {
      this.setState({sortField: event.sortField, sortOrder: event.sortOrder});
    }

    columnSort = (sortField: string) => {
      if (this.state.sortField === sortField) {
        const sortOrder = this.state.sortOrder === 1 ? -1 : 1;
        this.setState({sortOrder});
      } else {
        this.setState({sortField, sortOrder: 1});
      }
    }

    onPage = (event: any) => {
      this.setState({start: event.first});
    }

    overlayTemplate(rowData: any, column: any) {
      let vl: any;
      const valueField = (rowData.refRange || rowData.unit ) && column.field === 'value';
      const nameField = rowData.route && column.field === 'standardName';
      return <div style={{position: 'relative'}}>
        { column.field === 'value' && <span>{rowData.value}</span>}
        { column.field === 'standardName' && <span>{rowData.standardName}</span>}
        {(valueField || nameField)
        && <i className='pi pi-caret-down' style={styles.caretIcon} onClick={(e) => vl.toggle(e)}/>}
        <OverlayPanel className='labOverlay' ref={(el) => {vl = el; }}
                      showCloseIcon={true} dismissable={true}>
          {(rowData.refRange &&  column.field === 'value') &&
          <div style={{paddingBottom: '0.2rem'}}>Reference Range: {rowData.refRange}</div>}
          {(rowData.unit && column.field === 'value') &&
          <div>Units: {rowData.unit}</div>}
          {nameField &&
          <div>Route: {rowData.route}</div>}
        </OverlayPanel>
      </div>;
    }

    updateData = (event, colName, namesArray) => {
      const {domain, filterState} = this.props;
      let checkedItems = filterState.tabs[domain][colName];
      if (event.target.checked) {
        if (event.target.name === 'Select All') {
          checkedItems = namesArray.map(opt => opt.name);
        } else {
          checkedItems.push(event.target.name);
          if (namesArray.length - 1 === checkedItems.length) {
            // we have to add selectall when everything is selected
            checkedItems.push('Select All');
          }
        }
      } else {
        if (event.target.name === 'Select All') {
          checkedItems = [];
        } else {
          if (checkedItems.find(s => s === 'Select All')) {
            checkedItems.splice(checkedItems.indexOf('Select All'), 1);
          }
          checkedItems.splice(checkedItems.indexOf(event.target.name), 1);
        }
      }
      filterState.tabs[domain][colName] = checkedItems;
      this.props.getFilteredData(filterState);
      this.getErrorMessage(event.target.name);
    }

    filterData() {
      let {data, start} = this.state;
      const {
        domain,
        filterState,
        filterState: {global: {ageMin, ageMax, dateMin, dateMax, visits}, vocab}
      } = this.props;
      /* Global filters */
      if (dateMin || dateMax) {
        const min = dateMin ? Date.parse(dateMin) : 0;
        const max = dateMax ? Date.parse(dateMax) : 9999999999999;
        data = data.filter(item => {
          const itemDate = Date.parse(item.itemDate);
          return itemDate >= min && itemDate <= max;
        });
      }
      if (this.props.domain !== DomainType[DomainType.SURVEY] && (ageMin || ageMax)) {
        const min = ageMin || 0;
        const max = ageMax || 120;
        data = data.filter(item => item.ageAtEvent >= min && item.ageAtEvent <= max);
      }
      if (this.props.domain !== DomainType[DomainType.SURVEY]
        && this.props.domain !== DomainType[DomainType.PHYSICALMEASURE]
        && visits) {
        data = data.filter(item => visits === item.visitType);
      }
      /* Column filters */
      const checkedItems = filterState.tabs[domain];
      const empty = [];
      for (const col in checkedItems) {
        if (['domain', `${vocab}Vocabulary`, `${vocab}Code`].includes(col)
          && checkedItems[col].length
          && !(vocab === 'source' && domain === DomainType[DomainType.OBSERVATION])) {
          data = data.filter(row => checkedItems[col].includes(row[col]));
          empty.push(false);
        } else {
          empty.push(true);
        }
      }
      if (!empty.includes(false)) {
        if (checkedItems === undefined ||
          (vocab === 'source' && domain === DomainType[DomainType.OBSERVATION])) {
          // as some tab does not have filtered items but have data
          this.setState({filteredData: data, start: start});
        } else {
          this.setState({filteredData: []});
        }
      } else {
        if (data.length < start + rows) {
          start = Math.floor(data.length / rows) * rows;
        }
        this.setState({filteredData: data, start: start});
      }
    }

    getErrorMessage = (name?) => {
      const {data, filteredData} = this.state;
      const {domain, filterState} = this.props;
      const checkedItems = filterState.tabs[domain];
      if (data && data.length === 0) {
        return  'No ' + this.props.tabName + ' Data';
      } else {
        if (filteredData && filteredData.length === 0) {
          for (const col in checkedItems) {
            if (checkedItems[col].find( i => i !== name) || filteredData !== null) {
              // filtered data null  or if item selected from different participants not
              // exist in next/previous participants need to show this below message
              return 'There is data, but it is all currently hidden. Please check your filters';
            }
          }
        }
      }
    }

    filterCodes = (event: string) => {
      const {data} = this.state;
      const {domain, filterState, filterState: {vocab}} = this.props;
      const codeType = `${vocab}Code`;
      const checkedItems = filterState.tabs[domain][codeType];
      // TODO prevent duplicates and already selected options in results
      const codeResults = data.reduce((acc, item) => {
        if (item[codeType].includes(event)) {
          acc.push(item[codeType]);
        }
        return acc;
      }, []);
      this.setState({codeResults});
      console.log(event);
    }

    addCode = (event: any) => {
      // TODO remove from results, select option in main list when checked
      console.log(event.target.value);
    }

    filterTemplate(colName: string) {
      const {codeResults, data} = this.state;
      const {domain, filterState, filterState: {vocab}} = this.props;
      const checkedItems = filterState.tabs[domain];
      if (!data) {
        return {};
      }
      const counts = {total: 0};
      let options: Array<any>;
      data.forEach(item => {
        counts[item[colName]] = !!counts[item[colName]] ? counts[item[colName]] + 1 : 1;
        counts.total++;
      });
      switch (colName) {
        case 'domain':
          options = domains.map(name => {
            return {name, count: counts[name] || 0};
          });
          break;
        case `${vocab}Code`:
          options = Object.keys(counts).reduce((acc, name) => {
            if (name !== 'total') {
              acc.push({name, count: counts[name]});
            }
            return acc;
          }, []);
          break;
        case `${vocab}Vocabulary`:
          const vocabs = vocabOptions.getValue()[vocab];
          options = vocabs[domain] ? vocabs[domain].map(name => {
            return {name, count: counts[name] || 0};
          }) : [];
          break;
      }
      options.push({name: 'Select All', count: counts.total});
      if (checkedItems[colName].find(i => i === 'Select All')) {
        checkedItems[colName] = options.map(opt => opt.name);
      }
      const checkboxes = options.map((opt, i) => {
        return <React.Fragment key={i}>
          {opt.name !== 'Select All' && <div style={{padding: '0.3rem 0.4rem'}}>
            <input style={{width: '0.7rem',  height: '0.7rem'}} type='checkbox' name={opt.name}
                   checked={checkedItems[colName].includes(opt.name)}
                   onChange={($event) => this.updateData($event, colName, options)}/>
            <label style={{paddingLeft: '0.4rem'}}> {opt.name} ({opt.count}) </label>
          </div>}
        </React.Fragment>;
      });
      const codes = codeResults.map((opt, i) => {
        return <React.Fragment key={i}>
          <div style={{padding: '0.3rem 0.4rem'}}>
            <input style={{width: '0.7rem',  height: '0.7rem'}} type='checkbox' name={opt}
                   checked={false}
                   onChange={($event) => this.addCode($event)}/>
            <label style={{paddingLeft: '0.4rem'}}> {opt} </label>
          </div>
        </React.Fragment>;
      });
      let fl: any;
      return <span>
        <i className='pi pi-filter' onClick={(e) => fl.toggle(e)}/>
        <OverlayPanel style={{left: '359.531px!important'}} className='filterOverlay'
                      ref={(el) => {fl = el; }} showCloseIcon={true} dismissable={true}>
          {colName === `${vocab}Code` && <TextInput
            style={styles.codeSearch}
            onChange={this.codeInput} />}
          {codeResults.length && <div>{codes}</div>}
          <div style={{maxHeight: 'calc(100vh - 450px)', overflow: 'auto'}}>{checkboxes}</div>
          <div style={{borderTop: '1px solid #ccc', padding: '0.5rem 0.5rem'}}>
            <input style={{width: '0.7rem',  height: '0.7rem'}} type='checkbox' name='Select All'
                   checked={checkedItems[colName].includes('Select All')}
                   onChange={($event) => this.updateData($event, colName, options)}/>
            <label style={{paddingLeft: '0.4rem'}}> Select All ({counts.total}) </label>
          </div>
        </OverlayPanel>
      </span>;
    }

    rowExpansionTemplate = (rowData: any) => {
      const {data} = this.state;
      const conceptIdBasedData = fp.groupBy('standardConceptId', data);
      const unitsObj = fp.groupBy( 'unit', conceptIdBasedData[rowData.standardConceptId]);
      const unitKey = Object.keys(unitsObj);
      let valueArray;
      return <React.Fragment>
        <div style={styles.headerStyle}>{rowData.standardName}</div>
      <TabView className='unitTab'>
        {unitKey.map((k, i) => {
          const name = k === 'null' ? 'No Unit' : k;
          { valueArray = unitsObj[k].map(v => {
            return {
              values: parseInt(v.value, 10),
              date: v.itemDate,
            };
          }); }
          return <TabPanel header={name} key={i}>
            <ReviewDomainChartsComponent unitData={valueArray} />
          </TabPanel>;
        })}
      </TabView>
      </React.Fragment>;
    }
    hideGraphIcon = (rowData: any) => {
      const noConcept = rowData.standardName && rowData.standardName === 'No matching concept';
      return {'graphExpander' : noConcept};
    }

    render() {
      const {filteredData, loading, start, sortField, sortOrder} = this.state;
      let pageReportTemplate;
      if (filteredData !== null) {
        const lastRowOfPage = (start + rows) > filteredData.length
          ? start + rows - (start + rows - filteredData.length) : start + rows;
        pageReportTemplate = `${start + 1} - ${lastRowOfPage} of ${filteredData.length} records `;
      }
      let paginatorTemplate = 'CurrentPageReport';
      if (filteredData && filteredData.length > rows) {
        paginatorTemplate += ' PrevPageLink PageLinks NextPageLink';
      }
      const columns = this.props.columns.map((col) => {
        const asc = sortField === col.name && sortOrder === 1;
        const desc = sortField === col.name && sortOrder === -1;
        const colName = col.name === 'value' || col.name === 'standardName';
        const hasFilter = [
          'domain',
          'sourceVocabulary',
          'standardVocabulary',
          'sourceCode',
          'standardCode'
        ].includes(col.name);
        const isExpanderNeeded = col.name === 'graph'  &&
          (this.props.tabName === 'Vitals' || this.props.tabName === 'Labs');
        const overlayTemplate = colName && this.overlayTemplate;
        const header = <React.Fragment>
          <span
            onClick={() => this.columnSort(col.name)}
            style={styles.columnHeader}>
            {col.displayName}
          </span>
          {hasFilter && this.filterTemplate(col.name)}
          {(asc && !isExpanderNeeded) && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {(desc && !isExpanderNeeded) &&
          <i className='pi pi-arrow-down' style={styles.sortIcon} />}
        </React.Fragment>;

        return <Column
          expander={isExpanderNeeded}
          style={styles.tableBody}
          bodyStyle={isExpanderNeeded ? styles.graphColumnBody : styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          headerStyle={isExpanderNeeded && styles.graphStyle}
          sortable
          body={overlayTemplate}/>;
      });

      return <div style={styles.container}>
        <style>{css}</style>
        {filteredData && <DataTable
          expandedRows={this.state.expandedRows}
          onRowToggle={(e) => this.setState({expandedRows: e.data})}
          rowExpansionTemplate={this.rowExpansionTemplate}
          rowClassName = {this.hideGraphIcon}
          style={styles.table}
          value={filteredData}
          sortField={sortField}
          sortOrder={sortOrder}
          onSort={this.onSort}
          paginator
          paginatorTemplate={filteredData.length ? paginatorTemplate : ''}
          currentPageReportTemplate={filteredData.length ? pageReportTemplate : ''}
          onPage={this.onPage}
          first={start}
          rows={rows}
          totalRecords={filteredData.length}
          scrollable
          scrollHeight='calc(100vh - 350px)'
          autoLayout
          emptyMessage={this.getErrorMessage()}>
          {columns}
        </DataTable>}
        {loading && <SpinnerOverlay />}
      </div>;
    }
  }
);

@Component ({
  selector : 'app-detail-tab-table',
  template: '<div #root></div>'
})
export class DetailTabTableComponent extends ReactWrapperBase {
  @Input('tabName') tabName: DetailTabTableProps['tabName'];
  @Input('columns') columns: DetailTabTableProps['columns'];
  @Input('domain') domain: DetailTabTableProps['domain'];
  @Input('filterType') filterType: DetailTabTableProps['filterType'];
  @Input('participantId') participantId: DetailTabTableProps['participantId'];
  @Input('filterState') filterState: DetailTabTableProps['filterState'];
  @Input('getFilteredData') getFilteredData: DetailTabTableProps['getFilteredData'];
  @Input('updateState') updateState: DetailTabTableProps['updateState'];

  constructor() {
    super(DetailTabTable, [
      'tabName',
      'columns',
      'domain',
      'filterType',
      'participantId',
      'filterState',
      'getFilteredData',
      'updateState',
    ]);
  }
}
