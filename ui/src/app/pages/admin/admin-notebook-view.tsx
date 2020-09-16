import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {reactRouterUrlSearchParams} from 'app/utils/navigation';
import * as React from 'react';
import {useEffect, useState} from 'react';
import {useParams} from 'react-router';

const styles = reactStyles({
  heading: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 500,
  },
  notebook: {
    width: '100%',
    height: 'calc(100% - 40px)',
    position: 'absolute',
    border: 0
  },
  error: {
    marginLeft: 'auto',
    marginRight: 'auto',
    marginTop: '56px',
    background: colors.warning,
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    color: colors.white,
    display: 'flex',
    fontSize: '14px',
    fontWeight: 500,
    maxWidth: '550px',
    padding: '8px',
    textAlign: 'left'
  },
});

interface Props {
  workspaceNamespace: string;
  nbName: string;
  accessReason: string;
}

const AdminNotebookViewComponent = (props: Props) => {
  const {workspaceNamespace, nbName, accessReason} = props;
  const [notebookHtml, setHtml] = useState(undefined);
  const [workspaceName, setWorkspaceName] = useState(undefined);
  const [errorMessage, setErrorMessage] = useState(undefined);

  const Header = () => {
    const location = workspaceName ? `Workspace ${workspaceNamespace}/${workspaceName}` : workspaceNamespace;
    const link = <a href={`/admin/workspaces/${workspaceNamespace}`}>{location}</a>;

    return <div style={styles.heading}>Viewing {nbName} in {link} for reason: {accessReason}</div>;
  };

  useEffect(() => {
    workspaceAdminApi().getWorkspaceAdminView(workspaceNamespace)
      .then(workspaceAdminView => setWorkspaceName(workspaceAdminView.workspace.name));
  }, []);

  useEffect(() => {
    if (!accessReason || !accessReason.trim()) {
      setErrorMessage('Error: must include accessReason query parameter in URL');
    }
  }, [accessReason]);

  useEffect(() => {
    if (!accessReason || !accessReason.trim()) {
      return;
    }

    workspaceAdminApi().adminReadOnlyNotebook(workspaceNamespace, nbName, {reason: accessReason})
      .then(response => setHtml(response.html))
      .catch((e) => {
        if (e.status === 404) {
          setErrorMessage(`Notebook ${nbName} was not found`);
        } else if (e.status === 412) {
          setErrorMessage('Notebook is too large to display in preview mode');
        } else {
          setErrorMessage('Failed to render notebook preview due to unknown error');
        }
      });
  }, []);

  return <React.Fragment>
    <Header/>
    {errorMessage && <div style={styles.error}>{errorMessage}</div>}
    {notebookHtml && <iframe id='notebook-frame' style={styles.notebook} srcDoc={notebookHtml}/>}
  </React.Fragment>;
};

const AdminNotebookView = () => {
  const {workspaceNamespace, nbName} = useParams();
  const accessReason = reactRouterUrlSearchParams().get('accessReason');

  return <AdminNotebookViewComponent
      workspaceNamespace={workspaceNamespace}
      nbName={nbName}
      accessReason={accessReason}/>;
};

export {
  AdminNotebookView
};
