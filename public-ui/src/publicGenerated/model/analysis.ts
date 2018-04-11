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



export interface Analysis {
    /**
     * id analysis
     */
    analysisId: number;

    /**
     * analysis name
     */
    analysisName?: string;

    /**
     * usually concept name corresponding to stratum
     */
    stratum1Name?: string;

    /**
     * usually concept name corresponding to stratum
     */
    stratum2Name?: string;

    /**
     * usually concept name corresponding to stratum
     */
    stratum3Name?: string;

    /**
     * usually concept name corresponding to stratum
     */
    stratum4Name?: string;

    /**
     * usually concept name corresponding to stratum
     */
    stratum5Name?: string;

    /**
     * chart type to display for this analysis column pie box
     */
    chartType?: string;

    /**
     * data type of this analysis count or distribution
     */
    dataType?: string;

}
