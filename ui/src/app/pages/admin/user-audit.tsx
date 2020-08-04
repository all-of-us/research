import {AuditPageComponent} from 'app/components/admin/audit-page-component';
import {profileApi} from 'app/services/swagger-fetch-clients';
import * as React from 'react';
import {useParams} from 'react-router-dom';

const getAuditLog = (subject: string) => {
  const bqRowLimit = 1000; // Workspaces take many rows because of the Research Purpose fields
  return profileApi().getAuditLogEntries(subject, bqRowLimit);
};

const queryAuditLog = (subject: string) => {
  return getAuditLog(subject).then((queryResult) => {
    return {
      actions: queryResult.actions,
      sourceId: queryResult.userDatabaseId,
      query: queryResult.query,
      logEntries: queryResult.logEntries
    };
  });
};

const getNextAuditPath = (subject: string) => {
  return `/admin/user-audit/${subject}`;
};

// Single-user admin page isn't available yet, so go to the main users list page.
const getAdminPageUrl = (subject: string) => {
  return ['/admin/user'];
};

export const UserAudit = () => {
  const {username = ''} = useParams();
  return <AuditPageComponent auditSubjectType='User'
                             buttonLabel='Username without domain'
                             initialAuditSubject={username}
                             getNextAuditPath={getNextAuditPath}
                             queryAuditLog={queryAuditLog}
                             getAdminPageUrl={getAdminPageUrl}/>;
};
