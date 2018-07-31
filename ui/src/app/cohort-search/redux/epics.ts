import {Injectable} from '@angular/core';
import {Map} from 'immutable';
import {Epic} from 'redux-observable';
import {Observable} from 'rxjs/Observable';

/* tslint:disable:ordered-imports */
import {
  BEGIN_CRITERIA_REQUEST,
  BEGIN_ALL_CRITERIA_REQUEST,
  BEGIN_DRUG_CRITERIA_REQUEST,
  BEGIN_DRUG_AUTOCOMPLETE_REQUEST,
  BEGIN_INGREDIENT_REQUEST,
  CANCEL_CRITERIA_REQUEST,

  BEGIN_COUNT_REQUEST,
  CANCEL_COUNT_REQUEST,

  BEGIN_CHARTS_REQUEST,
  CANCEL_CHARTS_REQUEST,

  BEGIN_PREVIEW_REQUEST,
  BEGIN_ATTR_PREVIEW_REQUEST,

  RootAction,
  ActionTypes,
} from './actions/types';

import {
  loadCriteriaRequestResults,
  criteriaRequestError,

  loadCountRequestResults,
  countRequestError,

  loadChartsRequestResults,
  chartsRequestError,

  loadPreviewRequestResults,
  loadAttributePreviewRequestResults,
  previewRequestError,

  loadAutocompleteOptions,
  autocompleteRequestError,

  loadIngredients,
} from './actions/creators';

import {CohortSearchState} from './store';
/* tslint:enable:ordered-imports */

import {CohortBuilderService} from 'generated';

type CSEpic = Epic<RootAction, CohortSearchState>;
type CritRequestAction = ActionTypes[typeof BEGIN_CRITERIA_REQUEST];
type DrugCritRequestAction = ActionTypes[typeof BEGIN_DRUG_CRITERIA_REQUEST];
type AutocompleteRequestAction = ActionTypes[typeof BEGIN_DRUG_AUTOCOMPLETE_REQUEST];
type IngredientRequestAction = ActionTypes[typeof BEGIN_INGREDIENT_REQUEST];
type CountRequestAction = ActionTypes[typeof BEGIN_COUNT_REQUEST];
type ChartRequestAction = ActionTypes[typeof BEGIN_CHARTS_REQUEST];
type PreviewRequestAction = ActionTypes[typeof BEGIN_ATTR_PREVIEW_REQUEST];
type AttributePreviewRequestAction = ActionTypes[typeof BEGIN_PREVIEW_REQUEST];
const compare = (obj) => (action) => Map(obj).isSubset(Map(action));

/**
 * CohortSearchEpics
 *
 * Exposes functions (called `epics` by redux-observable) that listen in on the
 * stream of dispatched actions (exposed as an Observable) and attach handlers
 * to certain of them; this allows us to dispatch actions asynchronously.  This is
 * the interface between the application state and the backend API.
 *
 * TODO: clean up these funcs using the new lettable operators
 */
@Injectable()
export class CohortSearchEpics {
  constructor(private service: CohortBuilderService) {}

  fetchCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId}: CritRequestAction) => {
        return this.service.getCriteriaByTypeAndParentId(cdrVersionId, kind, parentId)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchAllCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_ALL_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId}: CritRequestAction) => {
        return this.service.getCriteriaByType(cdrVersionId, kind)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchDrugCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_DRUG_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId, subtype}: DrugCritRequestAction) => {
        return this.service.getCriteriaByTypeAndSubtype(cdrVersionId, kind, subtype)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchAutocompleteOptions: CSEpic = (action$) => (
    action$.ofType(BEGIN_DRUG_AUTOCOMPLETE_REQUEST).mergeMap(
      ({cdrVersionId, searchTerms}: AutocompleteRequestAction) => {
        return this.service.getDrugBrandOrIngredientByName(cdrVersionId, searchTerms)
          .map(result => loadAutocompleteOptions(result.items))
          .catch(e => Observable.of(autocompleteRequestError(e)));
      }
    )
  )

  fetchIngredientsForBrand: CSEpic = (action$) => (
    action$.ofType(BEGIN_INGREDIENT_REQUEST).mergeMap(
      ({cdrVersionId, conceptId}: IngredientRequestAction) => {
        return this.service.getDrugIngredientByConceptId(cdrVersionId, conceptId)
          .map(result => loadIngredients(result.items))
          .catch(e => Observable.of(autocompleteRequestError(e)));
      }
    )
  )

  fetchCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_COUNT_REQUEST).mergeMap(
      ({cdrVersionId, entityType, entityId, request}: CountRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadCountRequestResults(entityType, entityId, count))
        .race(action$
          .ofType(CANCEL_COUNT_REQUEST)
          .filter(compare({entityType, entityId}))
          .first())
        .catch(e => Observable.of(countRequestError(entityType, entityId, e)))
    )
  )

  previewCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_PREVIEW_REQUEST).switchMap(
      ({cdrVersionId, request}: PreviewRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadPreviewRequestResults(count))
        .catch(e => Observable.of(previewRequestError(e)))
    )
  )

  attributePreviewCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_ATTR_PREVIEW_REQUEST).switchMap(
      ({cdrVersionId, request}: AttributePreviewRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadAttributePreviewRequestResults(count))
        .catch(e => Observable.of(previewRequestError(e)))
    )
  )

  fetchChartData: CSEpic = (action$) => (
    action$.ofType(BEGIN_CHARTS_REQUEST).mergeMap(
      ({cdrVersionId, entityType, entityId, request}: ChartRequestAction) =>
      this.service.getChartInfo(cdrVersionId, request)
        .map(result => loadChartsRequestResults(entityType, entityId, result.items))
        .race(action$
          .ofType(CANCEL_CHARTS_REQUEST)
          .filter(compare({entityType, entityId}))
          .first())
        .catch(e => Observable.of(chartsRequestError(entityType, entityId, e)))
    )
  )
}
