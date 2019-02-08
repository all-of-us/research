import {Component, Input} from '@angular/core';
import {SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {PageFilterRequest, PageFilterType, SortOrder} from 'generated/fetch';
import {Column} from 'primereact/column';
import {Inplace, InplaceContent, InplaceDisplay} from 'primereact/components/inplace/Inplace';
import {DataTable} from 'primereact/datatable';
import {InputText} from 'primereact/inputtext';
import {Paginator} from 'primereact/paginator';
import * as React from 'react';

const styles = reactStyles({
  pDatatable: {
    fontSize: '12px',
    border: '1px solid #ccc'
  },
  pDatatableTbody: {
    padding: '5px',
    verticalAlign: 'top',
    borderLeft: 0,
    borderRight: 0,
    lineHeight: '0.6rem'
  }
});

export interface DetailTabTableProps {
  tabname: string;
  columns: Array<any>;
  domain: string;
  filterType: PageFilterType;
  participantId: number;
  cohortId: number;
  workspace: WorkspaceData;
}

export interface DetailTabTableState {
  data: Array<any>;
  rowsData: Array<any>;
  loading: boolean;
  totalCount: number;
  start: number;
  rows: number;
}

export const DetailTabTable = withCurrentWorkspace()(
  class extends React.Component<DetailTabTableProps, DetailTabTableState> {
    dt: any;
    constructor(props: DetailTabTableProps) {
      super(props);
      this.state = {
        data: null,
        rowsData: [],
        loading: true,
        totalCount: null,
        start: 0,
        rows: 25
      };
    }

    componentDidMount() {
      this.getParticipantData();
    }

    componentDidUpdate(prevProps) {
      if (prevProps.participantId !== this.props.participantId) {
        this.setState({
          data: null,
          loading: true
        });
        this.getParticipantData();
      }
    }

    onPageChange = (event: any) => {
      const startIndex = event.first;
      const endIndex = event.first + this.state.rows;
      this.setState({
        start: event.first,
        rowsData: this.state.data.slice(startIndex, endIndex)
      });
    }

    getParticipantData() {
      const {cdrVersionId, id, namespace} = this.props.workspace;
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
        this.props.cohortId,
        +cdrVersionId,
        this.props.participantId,
        pageFilterRequest
      ).then(response => {
        this.setState({
          data: response.items,
          rowsData: response.items.slice(0, this.state.rows),
          loading: false
        });
      });
    }

    columnFilter = (event) => {
      // TODO adjust layout to match current table and update pagination to match filtered data
      this.dt.filter(event.target.value, event.target.id, 'contains');
    }

    render() {
      const {loading, rows, rowsData, start} = this.state;
      const data = this.state.data || [];

      const columns = this.props.columns.map((col) => {
        const filter = <Inplace closable={true}>
          <InplaceDisplay>
            <i className='pi pi-filter' />
          </InplaceDisplay>
          <InplaceContent>
            <InputText
              className='p-inputtext p-column-filter'
              id={col.name}
              onChange={this.columnFilter} />
          </InplaceContent>
        </Inplace>;

        return <Column
          style={styles.pDatatableTbody}
          key={col.name}
          field={col.name}
          header={col.displayName}
          sortable={true}
          filter={true}
          filterElement={filter} />;
      });

      const footer = <Paginator
        first={start}
        rows={rows}
        totalRecords={data.length}
        onPageChange={this.onPageChange}
        template='FirstPageLink PrevPageLink CurrentPageReport NextPageLink LastPageLink' />;

      return <div style={{position: 'relative'}}>
        {data && <DataTable
          footer={footer}
          style={styles.pDatatable}
          ref={(el) => this.dt = el}
          value={rowsData}
          first={start}
          rows={rows}
          totalRecords={data.length}
          scrollable={true}
          scrollHeight='calc(100vh - 380px)'>
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
  @Input('cohortId') cohortId: DetailTabTableProps['cohortId'];

  constructor() {
    super(DetailTabTable, [
      'tabname',
      'columns',
      'domain',
      'filterType',
      'participantId',
      'cohortId',
    ]);
  }
}
