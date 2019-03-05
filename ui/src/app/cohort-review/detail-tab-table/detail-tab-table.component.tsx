import {Component, Input} from '@angular/core';
import {SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {urlParamsStore} from 'app/utils/navigation';
import {DomainType, PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {OverlayPanel} from 'primereact/overlaypanel';
import * as React from 'react';



const css = `
  body .p-datatable .p-sortable-column:not(.p-highlight):hover,
  body .p-datatable .p-sortable-column.p-highlight {
    color: #333333;
    background-color: #f4f4f4;
  }
  body .p-datatable .p-datatable-thead > tr > th {
    padding: 10px 5px 10px 10px;
    vertical-align: middle;
    background: #f4f4f4;
    border: 0;
    border-bottom: 1px solid #c8c8c8;
    border-left: 1px solid #c8c8c8;
  }
  body .p-datatable .p-datatable-thead > tr > th:first-of-type {
    border-left: 0;
  }
  body .p-datatable .p-datatable-tbody > tr:not(last-of-type) {
    border-bottom: 1px solid #c8c8c8;
  }
  // body .p-datatable .p-column-title {
  //   display: flex;
  // }
  .pi.pi-sort,
  .pi.pi-sort-up,
  .pi.pi-sort-down {
    display: none;
  }
  .p-datatable .p-datatable-scrollable-wrapper {
    border: 1px solid #c8c8c8;
  }
  .p-datatable .p-paginator.p-paginator-bottom {
    border: 0;
    margin-top: 20px;
    background: none;
    font-size: 12px;
    text-align: right;
  }
  body .p-paginator .p-paginator-pages {
    display: inline;
  }
  body .p-paginator .p-paginator-prev,
  body .p-paginator .p-paginator-next,
  body .p-paginator .p-paginator-pages .p-paginator-page {
    border: 1px solid #cccccc;
    border-radius: 3px;
    background: #fafafa;
    color: #2691D0;
    height: auto;
    width: auto;
    min-width: 0;
    padding: 7px;
    margin: 0 2px;
    line-height: 0.5rem;
  }
  body .p-paginator .p-paginator-prev,
  body .p-paginator .p-paginator-next {
    height: 28px;
    width: 24px;
  }
  body .p-paginator .p-paginator-pages .p-paginator-page:focus {
    box-shadow: 0;
  }
  body .p-paginator .p-paginator-pages .p-paginator-page.p-highlight {
    background: #fafafa;
    color: rgba(0, 0, 0, .5);
  }
  body .labOverlay.p-overlaypanel .p-overlaypanel-close {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .labOverlay.p-overlaypanel {
    top: 19px!important;
    left: 0px!important;
    width:9.5rem;
  }
  body .labOverlay.p-overlaypanel .p-overlaypanel-close:hover {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .labOverlay.p-overlaypanel .p-overlaypanel-content {
    padding: 0.6rem 0.6rem;
    font-size: 13px;
  }
    body .filterOverlay.p-overlaypanel .p-overlaypanel-close {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .filterOverlay.p-overlaypanel {
    top: 1.6rem!important;
    left: auto!important;
    width:9.5rem;
  }
  body .filterOverlay.p-overlaypanel .p-overlaypanel-close:hover {
    top: 0.231em;
    right: 0.231em;
    background-color: white;
    color: #0086C1;
  }
  body .filterOverlay.p-overlaypanel .p-overlaypanel-content {
    padding: 0.7em 0em;
    font-size: 13px;
  }
  `;

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
    padding: '5px',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem'
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
  }
});
const rows = 25;

export interface DetailTabTableProps {
  tabname: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
  workspace: WorkspaceData;
  filteredTab: any;
  getFilteredData: Function;
}

export interface DetailTabTableState {
  data: Array<any>;
  filteredData: Array<any>;
  loading: boolean;
  start: number;
  sortField: string;
  sortOrder: number;
  checkedItems: any;

}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<DetailTabTableProps, DetailTabTableState> {
    test: boolean;
    constructor(props: DetailTabTableProps) {
      super(props);
      this.state = {
        data: null,
        filteredData: null,
        loading: true,
        start: 0,
        sortField: null,
        sortOrder: 1,
        checkedItems: props.filteredTab,
      };
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
      const {checkedItems, data} = this.state;
      if (event.target.checked) {
        if (event.target.name === 'SelectAll') {
          checkedItems[colName] = namesArray ;
        } else {
          checkedItems[colName].push(event.target.name);
          if (namesArray.length - 1 === checkedItems[colName].length) {
            // we have to add selectall when everything is selected
            checkedItems[colName].push('SelectAll');
          }
        }
      } else {
        if (event.target.name === 'SelectAll') {
          checkedItems[colName] = [];
          this.setState({filteredData: checkedItems});
        } else {
          if (checkedItems[colName].find(s => s === 'SelectAll')) {
            checkedItems[colName]
              .splice(checkedItems[colName]
                .indexOf('SelectAll'), 1);
          }
          checkedItems[colName]
            .splice(checkedItems[colName]
              .indexOf(event.target.name), 1);
        }
      }
      this.getErrorMessage(event.target.name);
      this.setState({checkedItems: checkedItems});
      this.props.getFilteredData(colName, checkedItems);
      if (data) {
        this.filterData();
      }
    }

    filterData() {
      const {checkedItems} = this.state;
      let {data, start} = this.state;
      const empty = [];

      for (const col in checkedItems) {
        if (checkedItems[col].length) {
          data = data.filter(row => checkedItems[col].includes(row[col]));
          empty.push(false);
        } else {
          empty.push(true);
        }
      }
      if (!empty.includes(false)) {
        if (data && data.length > 0) {
          // if selected tab does not have cheked items but have data
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
      const {data, checkedItems, filteredData} = this.state;
      if (data && data.length === 0) {
        return  'No ' + this.props.tabname + ' Data';
      } else {
        if ((filteredData && filteredData.length === 0)) {
          for (const col in checkedItems) {
            if (checkedItems[col].find( i => i !== name)) {
              return 'There is data, but it is all currently hidden. Please check your filters';
            }
          }
        }
      }
    }

    addAllOption(arr) {
      arr.push('SelectAll');
    }

    getColumnValue(colName) {
      const {data, checkedItems} = this.state;
      let names = [];
      if (data) {
        names = [...fp.uniq(data.map(item => {
          if (colName === 'standardVocabulary') {
            return item.standardVocabulary;
          } else if (colName === 'domain') {
            return item.domain;
          }
        }))];
      }
      this.addAllOption(names);
      if (checkedItems[colName].find(i => i === 'SelectAll')) {
        checkedItems[colName] = names;
      }
      let fl: any;

      return ( <span>  {data && <i className='pi pi-filter' onClick={(e) => fl.toggle(e)}/>}
        <OverlayPanel style={{left: '359.531px!important'}} className='filterOverlay'
                      ref={(el) => {fl = el; }} showCloseIcon={true} dismissable={true}>
          {names.map((i, index) => (
            <div key={index} style={{borderTop: i === 'SelectAll' ? '1px solid #ccc' : 'none',
              padding: i === 'SelectAll' ? '0.5rem 0.5rem' : '0.3rem 0.4rem'}} >
              <input style={{width: '0.7rem',  height: '0.7rem'}} type='checkbox' name={i}
                     checked={checkedItems[colName].includes(i)}
                     onChange={($event) => this.updateData($event, colName, names)}/>
              <label style={{paddingLeft: '0.4rem'}}> {i} </label>
            </div>
          ))}
        </OverlayPanel>
      </span>);
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
        const filterColName = col.name === 'standardVocabulary' || col.name === 'domain';
        const header = <React.Fragment>
          <span
            onClick={() => this.columnSort(col.name)}
            style={styles.columnHeader}>
            {col.displayName}
          </span>
          {asc && <i className='pi pi-arrow-up' style={styles.sortIcon} />}
          {desc && <i className='pi pi-arrow-down' style={styles.sortIcon} />}
        </React.Fragment>;

        return <Column
          style={styles.tableBody}
          bodyStyle={styles.columnBody}
          key={col.name}
          field={col.name}
          header={header}
          sortable
          filter={filterColName && true}
          filterElement= {filterColName && this.getColumnValue(col.name)}
          body={colName && this.overlayTemplate}/>;
      });

      return <div style={styles.container}>
        <style>{css}</style>
        {filteredData && <DataTable
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
  @Input('tabname') tabname: DetailTabTableProps['tabname'];
  @Input('columns') columns: DetailTabTableProps['columns'];
  @Input('domain') domain: DetailTabTableProps['domain'];
  @Input('filterType') filterType: DetailTabTableProps['filterType'];
  @Input('participantId') participantId: DetailTabTableProps['participantId'];
  @Input('filteredTab') filteredTab: DetailTabTableProps['filteredTab'];
  @Input('getFilteredData') getFilteredData: DetailTabTableProps['getFilteredData'];

  constructor() {
    super(DetailTabTable, [
      'tabname',
      'columns',
      'domain',
      'filterType',
      'participantId',
      'filteredTab',
      'getFilteredData',
    ]);
  }
}

