import {ActionsObservable} from 'redux-observable';
import {AnyAction} from 'redux';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions as Actions} from './actions';
import {CohortBuilderService} from 'generated';

/**
 * CohortSearchEpics
 *
 * Exposes functions (called `epics` by redux-observable) that listen in on the
 * stream of dispatched actions (exposed as an Observable) and attach handlers
 * to certain of them; this allows us to dispatch actions asynchronously.  This is
 * the interface between the application state and the backend API.
 */
@Injectable()
export class CohortSearchEpics {
  constructor(private service: CohortBuilderService) {}

  fetchCriteria = (action$: ActionsObservable<AnyAction>) => (
    action$.ofType(Actions.FETCH_CRITERIA).mergeMap(
      ({critType, parentId}) => {
        let _type = critType.match(/^DEMO.*/i) ? 'DEMO' : critType;
        return this.service.getCriteriaByTypeAndParentId(_type, parentId)
          .map(result => ({type: Actions.LOAD_CRITERIA, children: result.items, critType, parentId}))
          .catch(error => Observable.of({type: Actions.ERROR, error, critType, parentId}))
      }
    )
  )

  fetchSearchResults = (action$: ActionsObservable<AnyAction>) => (
    action$.ofType(Actions.FETCH_SEARCH_RESULTS).mergeMap(
      ({request, sgiPath}) =>
      this.service.searchSubjects(request)
        .map(results => ({type: Actions.LOAD_SEARCH_RESULTS, results, sgiPath}))
        .catch(error => Observable.of({type: Actions.ERROR, error, sgiPath}))
    )
  )

  recalculateCounts = (action$: ActionsObservable<AnyAction>) => (
    action$.ofType(
      Actions.LOAD_SEARCH_RESULTS,
      Actions.REMOVE_SEARCH_GROUP,
      Actions.REMOVE_GROUP_ITEM,
      Actions.REMOVE_CRITERIA,
    ).map(() => ({type: Actions.RECALCULATE_COUNTS}))
  )
}
