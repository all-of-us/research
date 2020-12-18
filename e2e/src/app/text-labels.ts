import {config} from 'resources/workbench-config';

export enum PageUrl {
   Home = config.uiBaseUrl,
   Workspaces = config.uiBaseUrl + config.workspacesUrlPath,
   Admin = config.uiBaseUrl + config.adminUrlPath,
}

export enum WorkspaceAccessLevel {
   Owner = 'OWNER',
   Reader = 'READER',
   Writer = 'WRITER',
}

// Options in dropdown select.
export enum Option {
   CopyToAnotherWorkspace = 'Copy to another Workspace',
   Delete = 'Delete',
   Duplicate  = 'Duplicate',
   Edit = 'Edit',
   Rename = 'Rename',
   RenameDataset = 'Rename Dataset',
   Review = 'Review',
   Share = 'Share',
   exportToNotebook = 'Export to Notebook',
}

// Button or link text labels.
export enum LinkText {
   AddSelection = 'Add Selection',
   AddThis = 'ADD THIS',
   ApplyModifiers = 'APPLY MODIFIERS',
   BackToCohort = 'Back to cohort',
   BackToReviewSet = 'Back to review set',
   Calculate = 'Calculate',
   Cancel = 'Cancel',
   Confirm = 'Confirm',
   Continue = 'Continue',
   Copy = 'Copy',
   Create = 'Create',
   CreateAnotherCohort = 'Create another Cohort',
   CreateAnotherConceptSet = 'Create another Concept Set',
   CreateCohort = 'Create Cohort',
   CreateDataset = 'Create a Dataset',
   CreateNewNotebook = 'New Notebook',
   CreateNewWorkspace = 'Create a New Workspace',
   CreateNotebook = 'Create Notebook',
   CreateReviewSets = 'Create Review Sets',
   CreateSet = 'CREATE SET',
   CreateWorkspace = 'Create Workspace',
   Customize = 'Customize',
   Delete = 'Delete',
   DeleteCohort = 'Delete Cohort',
   DeleteConceptSet = 'Delete Concept Set',
   DeleteDataset = 'Delete Dataset',
   DeleteEnvironment = 'Delete Environment',
   DeleteNotebook = 'Delete Notebook',
   DeleteWorkspace = 'Delete Workspace',
   DiscardChanges = 'Discard Changes',
   DuplicateWorkspace = 'Duplicate Workspace',
   ExportAndOpen = 'Export and Open',
   Finish = 'Finish',
   FinishAndReview = 'Finish & Review',
   GoToCopiedConceptSet = 'Go to Copied Concept Set',
   GoToCopiedNotebook = 'Go to Copied Notebook',
   KeepEditing = 'Keep Editing',
   Next = 'Next',
   Rename = 'Rename',
   RenameCohort = 'Rename Cohort',
   RenameCohortReview = 'Rename Cohort Review',
   RenameConceptSet = 'Rename Concept Set',
   RenameDataset = 'Rename Dataset',
   RenameNotebook = 'Rename Notebook',
   Save = 'Save',
   SaveCohort = 'Save Cohort',
   SaveConceptSet = 'Save Concept Set',
   SaveCriteria = 'Save Criteria',
   SeeCodePreview = 'See Code Preview',
   StayHere = 'Stay Here',
   Submit = 'Submit',
   Update = 'Update',
}

// Notebook programming language.
export enum Language {
   Python = 'Python',
   R = 'R',
}

// Data resource card.
export enum ResourceCard {
   Cohort = 'Cohort',
   ConceptSet = 'Concept Set',
   Notebook = 'Notebook',
   Dataset = 'Dataset',
   CohortReview = 'Cohort Review',
}
