/**
 * AllOfUs Public API
 * The API for the AllOfUs data browser and public storefront.
 *
 * OpenAPI spec version: 0.1.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

/* tslint:disable:no-unused-variable member-ordering */

import { Inject, Injectable, Optional }                      from '@angular/core';
import { Http, Headers, URLSearchParams }                    from '@angular/http';
import { RequestMethod, RequestOptions, RequestOptionsArgs } from '@angular/http';
import { Response, ResponseContentType }                     from '@angular/http';

import { Observable }                                        from 'rxjs/Observable';
import '../rxjs-operators';

import { AchillesResult } from '../model/achillesResult';
import { ConceptAnalysisListResponse } from '../model/conceptAnalysisListResponse';
import { ConceptListResponse } from '../model/conceptListResponse';
import { DbDomainListResponse } from '../model/dbDomainListResponse';
import { QuestionConceptListResponse } from '../model/questionConceptListResponse';
import { SearchConceptsRequest } from '../model/searchConceptsRequest';

import { BASE_PATH, COLLECTION_FORMATS }                     from '../variables';
import { Configuration }                                     from '../configuration';


@Injectable()
export class DataBrowserService {

    protected basePath = 'https://public-api.pmi-ops.org';
    public defaultHeaders: Headers = new Headers();
    public configuration: Configuration = new Configuration();

    constructor(protected http: Http, @Optional()@Inject(BASE_PATH) basePath: string, @Optional() configuration: Configuration) {
        if (basePath) {
            this.basePath = basePath;
        }
        if (configuration) {
            this.configuration = configuration;
			this.basePath = basePath || configuration.basePath || this.basePath;
        }
    }

    /**
     * 
     * Extends object by coping non-existing properties.
     * @param objA object to be extended
     * @param objB source object
     */
    private extendObj<T1,T2>(objA: T1, objB: T2) {
        for(let key in objB){
            if(objB.hasOwnProperty(key)){
                (objA as any)[key] = (objB as any)[key];
            }
        }
        return <T1&T2>objA;
    }

    /**
     * @param consumes string[] mime-types
     * @return true: consumes contains 'multipart/form-data', false: otherwise
     */
    private canConsumeForm(consumes: string[]): boolean {
        const form = 'multipart/form-data';
        for (let consume of consumes) {
            if (form === consume) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets analysis results for concept
     * @param conceptIds concept id
     */
    public getConceptAnalysisResults(conceptIds: Array<string>, extraHttpRequestParams?: any): Observable<ConceptAnalysisListResponse> {
        return this.getConceptAnalysisResultsWithHttpInfo(conceptIds, extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets list of analysis definitions
     */
    public getDbDomains(extraHttpRequestParams?: any): Observable<DbDomainListResponse> {
        return this.getDbDomainsWithHttpInfo(extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets domain filters
     */
    public getDomainFilters(extraHttpRequestParams?: any): Observable<DbDomainListResponse> {
        return this.getDomainFiltersWithHttpInfo(extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets the domain filters with the count of matched concepts
     * @param searchWord search key word
     */
    public getDomainSearchResults(searchWord: string, extraHttpRequestParams?: any): Observable<DbDomainListResponse> {
        return this.getDomainSearchResultsWithHttpInfo(searchWord, extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets the domain filters with the count of matched concepts
     */
    public getDomainTotals(extraHttpRequestParams?: any): Observable<DbDomainListResponse> {
        return this.getDomainTotalsWithHttpInfo(extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets parent concepts for the given concept
     * @param conceptId concept id to get maps to concepts
     */
    public getParentConcepts(conceptId: number, extraHttpRequestParams?: any): Observable<ConceptListResponse> {
        return this.getParentConceptsWithHttpInfo(conceptId, extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets results for an analysis id and stratum
     */
    public getParticipantCount(extraHttpRequestParams?: any): Observable<AchillesResult> {
        return this.getParticipantCountWithHttpInfo(extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Get children of the given concept
     * @param conceptId concept id to get maps to concepts
     * @param minCount minimum source count
     */
    public getSourceConcepts(conceptId: number, minCount?: number, extraHttpRequestParams?: any): Observable<ConceptListResponse> {
        return this.getSourceConceptsWithHttpInfo(conceptId, minCount, extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets survey list
     */
    public getSurveyList(extraHttpRequestParams?: any): Observable<DbDomainListResponse> {
        return this.getSurveyListWithHttpInfo(extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets list of quetions with results for a survey
     * @param surveyConceptId survey concept id
     */
    public getSurveyResults(surveyConceptId: string, extraHttpRequestParams?: any): Observable<QuestionConceptListResponse> {
        return this.getSurveyResultsWithHttpInfo(surveyConceptId, extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }

    /**
     * Gets list of matched concepts
     * @param request search concept request
     */
    public searchConcepts(request?: SearchConceptsRequest, extraHttpRequestParams?: any): Observable<ConceptListResponse> {
        return this.searchConceptsWithHttpInfo(request, extraHttpRequestParams)
            .map((response: Response) => {
                if (response.status === 204) {
                    return undefined;
                } else {
                    return response.json() || {};
                }
            });
    }


    /**
     * 
     * Gets analysis results for concept
     * @param conceptIds concept id
     */
    public getConceptAnalysisResultsWithHttpInfo(conceptIds: Array<string>, extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/concept-analysis-results';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845

        // verify required parameter 'conceptIds' is not null or undefined
        if (conceptIds === null || conceptIds === undefined) {
            throw new Error('Required parameter conceptIds was null or undefined when calling getConceptAnalysisResults.');
        }
        if (conceptIds) {
            queryParameters.set('concept-ids', conceptIds.join(COLLECTION_FORMATS['csv']));
        }


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets list of analysis definitions
     */
    public getDbDomainsWithHttpInfo(extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/db-domains';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets domain filters
     */
    public getDomainFiltersWithHttpInfo(extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/domain-filters';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets the domain filters with the count of matched concepts
     * @param searchWord search key word
     */
    public getDomainSearchResultsWithHttpInfo(searchWord: string, extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/domain-search';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845

        // verify required parameter 'searchWord' is not null or undefined
        if (searchWord === null || searchWord === undefined) {
            throw new Error('Required parameter searchWord was null or undefined when calling getDomainSearchResults.');
        }
        if (searchWord !== undefined) {
            queryParameters.set('searchWord', <any>searchWord);
        }


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets the domain filters with the count of matched concepts
     */
    public getDomainTotalsWithHttpInfo(extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/domain-totals';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets parent concepts for the given concept
     * @param conceptId concept id to get maps to concepts
     */
    public getParentConceptsWithHttpInfo(conceptId: number, extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/parent-concepts';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845

        // verify required parameter 'conceptId' is not null or undefined
        if (conceptId === null || conceptId === undefined) {
            throw new Error('Required parameter conceptId was null or undefined when calling getParentConcepts.');
        }
        if (conceptId !== undefined) {
            queryParameters.set('concept_id', <any>conceptId);
        }


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets results for an analysis id and stratum
     */
    public getParticipantCountWithHttpInfo(extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/participant-count';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Get children of the given concept
     * @param conceptId concept id to get maps to concepts
     * @param minCount minimum source count
     */
    public getSourceConceptsWithHttpInfo(conceptId: number, minCount?: number, extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/source-concepts';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845

        // verify required parameter 'conceptId' is not null or undefined
        if (conceptId === null || conceptId === undefined) {
            throw new Error('Required parameter conceptId was null or undefined when calling getSourceConcepts.');
        }
        if (conceptId !== undefined) {
            queryParameters.set('concept_id', <any>conceptId);
        }

        if (minCount !== undefined) {
            queryParameters.set('minCount', <any>minCount);
        }


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets survey list
     */
    public getSurveyListWithHttpInfo(extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/survey-list';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets list of quetions with results for a survey
     * @param surveyConceptId survey concept id
     */
    public getSurveyResultsWithHttpInfo(surveyConceptId: string, extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/survey-results';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845

        // verify required parameter 'surveyConceptId' is not null or undefined
        if (surveyConceptId === null || surveyConceptId === undefined) {
            throw new Error('Required parameter surveyConceptId was null or undefined when calling getSurveyResults.');
        }
        if (surveyConceptId !== undefined) {
            queryParameters.set('survey_concept_id', <any>surveyConceptId);
        }


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Get,
            headers: headers,
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

    /**
     * 
     * Gets list of matched concepts
     * @param request search concept request
     */
    public searchConceptsWithHttpInfo(request?: SearchConceptsRequest, extraHttpRequestParams?: any): Observable<Response> {
        const path = this.basePath + '/v1/databrowser/searchConcepts';

        let queryParameters = new URLSearchParams();
        let headers = new Headers(this.defaultHeaders.toJSON()); // https://github.com/angular/angular/issues/6845


        // to determine the Accept header
        let produces: string[] = [
            'application/json'
        ];

            
        headers.set('Content-Type', 'application/json');

        let requestOptions: RequestOptionsArgs = new RequestOptions({
            method: RequestMethod.Post,
            headers: headers,
            body: request == null ? '' : JSON.stringify(request), // https://github.com/angular/angular/issues/10612
            search: queryParameters,
            withCredentials:this.configuration.withCredentials
        });
        // https://github.com/swagger-api/swagger-codegen/issues/4037
        if (extraHttpRequestParams) {
            requestOptions = (<any>Object).assign(requestOptions, extraHttpRequestParams);
        }

        return this.http.request(path, requestOptions);
    }

}
