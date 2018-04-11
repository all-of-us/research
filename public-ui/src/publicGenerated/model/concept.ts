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



export interface Concept {
    /**
     * id of the concept
     */
    conceptId: number;

    /**
     * name of concept
     */
    conceptName: string;

    /**
     * domain of concept
     */
    domainId: string;

    /**
     * vocabulary of concept
     */
    vocabularyId: string;

    /**
     * original vocab code of concept
     */
    conceptCode: string;

    /**
     * class of concept
     */
    conceptClassId: string;

    /**
     * standard concept value 1 char
     */
    standardConcept: string;

    /**
     * est count in the cdr
     */
    countValue?: number;

    /**
     * prevalence among participants percent count divided num participants
     */
    prevalence?: number;

}
