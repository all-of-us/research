import {AuditActionCardListView} from 'app/components/admin/audit-card-list-view';
import {Navigate} from 'app/components/app-router';
import {Button} from 'app/components/buttons';
import {NumberInput, TextInputWithLabel} from 'app/components/inputs';
import colors from 'app/styles/colors';
import {actionToString} from 'app/utils/audit-utils';
import * as fp from 'lodash/fp';
import * as React from 'react';
import { useParams } from 'react-router-dom';
import {AuditLogEntry} from '../../../generated';
import {AuditAction} from '../../../generated/model/auditAction';



const {useEffect, useState} = React;

// The APIs for user and workspace audit are slightly different, and typed differently,
// because of the need to return different database IDs and not get them confused (in future endpoints).
// This interface allows an easy mapping from both real query response types.
export interface GenericAuditQueryResult {
  /**
   * Re-constructed objects for each composite action in the query results.
   */
  actions?: Array<AuditAction>;
  logEntries: Array<AuditLogEntry>;
  /**
   * Copy of the query used in BigQuery to assist in further exploration or debugging.
   */
  query: string;
  /**
   * ID in the MySQL database and BigQuery Audit Database for the this qyery. Currently either
   * a userId or workspaceId as appropriate. This may be needed for situations (such as workspacd audit)
   * where it's not obvious in the responses which workspace is the one you audited.
   */
  sourceId: number;
}

// Common properties for User & Workspace (and similar future pages).
// Assmptions: the path parameter is called initially with the audit API subject.
//   (this will need revisiting for multi-subject queries)
export interface AuditPageProps {
  initialAuditSubject?: string;
  auditSubjectType: string;
  queryAuditLog: (subject: string) => Promise<GenericAuditQueryResult>;
  getNextAuditPath: (subject: string) => string;
  debug: boolean;
}

const UserInput = ({initialAuditSubject, auditSubjectType, getNextAuditPath}) => {
  const [auditSubject, setAuditSubject] = useState(initialAuditSubject);
  const [loadNextSubject, setLoadNextSubject] = useState(false);

  useEffect(() => {
    loadNextSubject && setLoadNextSubject(false);
  }, [loadNextSubject]);

  return <React.Fragment>
    {loadNextSubject && <Navigate to={getNextAuditPath(auditSubject)}/>}
    <TextInputWithLabel
      containerStyle={{display: 'inline-block'}}
      style={{width: '15rem', margin: '1rem'}}
      labelText = {auditSubjectType}
      value = {auditSubject}
      onChange = {setAuditSubject}
    />
    <Button style={{height: '1.5rem', margin: '0.25rem 0.5rem'}} disabled={fp.isEmpty(auditSubject)} onClick={() => setLoadNextSubject(true)}>
    Audit
    </Button>
  </React.Fragment>;
};

export const AuditPageComponent = (props: AuditPageProps) => {
  const {initialAuditSubject, queryAuditLog, getNextAuditPath, debug, auditSubjectType} = props;
  const [loading, setLoading] = useState(true);
  const [queryResult, setQueryResult] = useState<GenericAuditQueryResult>({actions: [], logEntries: [], sourceId: 0, query: ''});
  const [displayNum, setDisplayNum] = useState(20);
  const {actions, sourceId, query} = queryResult;

  useEffect(() => {
    if (debug) {
      console.log(fp.map(actionToString, actions).join('\n'));
      console.log(actions);
      console.log(query);
    }
  }, [debug, queryResult]);

  useEffect(() => {
    const getLogEntries = async() => {
      setLoading(true);
      const rowLimit = 500; // rows to fetch from BigQuery audit table. It's not possible to request a specific number of Actions.
      try {
        setQueryResult(await queryAuditLog(initialAuditSubject));
      } catch (e) {
        console.warn(`Error retrieving audit query results for ${initialAuditSubject}`);
      }
      setLoading(false);
    };

    getLogEntries();
  }, [initialAuditSubject]);

  const getTitle = () => {
    const timesMillis = actions.map(action => new Date(action.actionTime).getTime());
    const minTime = new Date(Math.min(...timesMillis)).toDateString();
    const maxTime = new Date(Math.max(...timesMillis)).toDateString();
    return initialAuditSubject
      ? `${auditSubjectType} ${initialAuditSubject} ID ${sourceId} Audits from ${minTime} to ${maxTime}`
      : `No ${auditSubjectType} selected`;
  };

  return !loading
    ? <React.Fragment>
        <div style={{marginLeft: '1rem'}}>
          <UserInput initialAuditSubject={initialAuditSubject} auditSubjectType={auditSubjectType} getNextAuditPath={getNextAuditPath}/>
          <div>
            <label style={{
              fontSize: 14,
              display: 'block',
              color: colors.primary,
              lineHeight: '22px',
              fontWeight: 600,
              marginRight: '0.25rem'
            }}>Number of Actions to Display</label>
            <NumberInput value={displayNum} min={1} style={{width: '4rem'}} onChange={setDisplayNum}/>
          </div>
          <div>{getTitle()}</div>
        </div>
        <AuditActionCardListView actions={fp.slice(0, displayNum, actions)}/>
      </React.Fragment>
    : <div>Loading Audit for {initialAuditSubject}...</div>;
};

// TODO (extra credit)
// - make all cards in the CardListView  collapsible to a summary, e.g. time + a list of action types
// - make the list of AuditPropertyChangeCards collapsible locally (expand/collapse all would  be nice too)
// - add a filter on  ActionType to all the cards in the view. All selected actions must be present
//   in a card for it to render. I.e. we don't want to hide individual EventBundleCards I dont' think.
// - pagination based on the `before` property in the API.
//     * first fetch the 20 (say) most recent actions (limit = 20, after = null,  before = null)
//     * Take timeestamp of the eoldest card reeturned, set `before` to that value and call again
//     - repeat until no results
// - make a generic CardListView
// - use common card style stuff like React.FunctionComponent<WorkspaceCardMenuProps>
