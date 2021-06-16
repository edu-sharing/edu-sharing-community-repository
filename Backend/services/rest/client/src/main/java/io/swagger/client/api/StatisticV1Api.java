/*
 * edu-sharing Repository REST API
 * The public restful API of the edu-sharing repository.
 *
 * OpenAPI spec version: 1.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package io.swagger.client.api;

import io.swagger.client.ApiCallback;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.Configuration;
import io.swagger.client.Pair;
import io.swagger.client.ProgressRequestBody;
import io.swagger.client.ProgressResponseBody;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;


import io.swagger.client.model.ErrorResponse;
import io.swagger.client.model.Filter;
import io.swagger.client.model.Statistics;
import io.swagger.client.model.StatisticsGlobal;
import io.swagger.client.model.Tracking;
import io.swagger.client.model.TrackingNode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticV1Api {
    private ApiClient apiClient;

    public StatisticV1Api() {
        this(Configuration.getDefaultApiClient());
    }

    public StatisticV1Api(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for get
     * @param context context, the node where to start (required)
     * @param body filter (required)
     * @param properties properties (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getCall(String context, Filter body, List<String> properties, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/statistic/v1/facettes/{context}"
            .replaceAll("\\{" + "context" + "\\}", apiClient.escapeString(context.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (properties != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("multi", "properties", properties));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getValidateBeforeCall(String context, Filter body, List<String> properties, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'context' is set
        if (context == null) {
            throw new ApiException("Missing the required parameter 'context' when calling get(Async)");
        }
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new ApiException("Missing the required parameter 'body' when calling get(Async)");
        }
        

        com.squareup.okhttp.Call call = getCall(context, body, properties, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get statistics of repository.
     * Statistics.
     * @param context context, the node where to start (required)
     * @param body filter (required)
     * @param properties properties (optional)
     * @return Statistics
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public Statistics get(String context, Filter body, List<String> properties) throws ApiException {
        ApiResponse<Statistics> resp = getWithHttpInfo(context, body, properties);
        return resp.getData();
    }

    /**
     * Get statistics of repository.
     * Statistics.
     * @param context context, the node where to start (required)
     * @param body filter (required)
     * @param properties properties (optional)
     * @return ApiResponse&lt;Statistics&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Statistics> getWithHttpInfo(String context, Filter body, List<String> properties) throws ApiException {
        com.squareup.okhttp.Call call = getValidateBeforeCall(context, body, properties, null, null);
        Type localVarReturnType = new TypeToken<Statistics>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get statistics of repository. (asynchronously)
     * Statistics.
     * @param context context, the node where to start (required)
     * @param body filter (required)
     * @param properties properties (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getAsync(String context, Filter body, List<String> properties, final ApiCallback<Statistics> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getValidateBeforeCall(context, body, properties, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<Statistics>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getGlobalStatistics
     * @param group primary property to build facettes and count+group values (optional)
     * @param subGroup additional properties to build facettes and count+sub-group values (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getGlobalStatisticsCall(String group, List<String> subGroup, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/statistic/v1/public";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (group != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("group", group));
        if (subGroup != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("multi", "subGroup", subGroup));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getGlobalStatisticsValidateBeforeCall(String group, List<String> subGroup, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        

        com.squareup.okhttp.Call call = getGlobalStatisticsCall(group, subGroup, progressListener, progressRequestListener);
        return call;

    }

    /**
     * Get stats.
     * Get global statistics for this repository.
     * @param group primary property to build facettes and count+group values (optional)
     * @param subGroup additional properties to build facettes and count+sub-group values (optional)
     * @return StatisticsGlobal
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public StatisticsGlobal getGlobalStatistics(String group, List<String> subGroup) throws ApiException {
        ApiResponse<StatisticsGlobal> resp = getGlobalStatisticsWithHttpInfo(group, subGroup);
        return resp.getData();
    }

    /**
     * Get stats.
     * Get global statistics for this repository.
     * @param group primary property to build facettes and count+group values (optional)
     * @param subGroup additional properties to build facettes and count+sub-group values (optional)
     * @return ApiResponse&lt;StatisticsGlobal&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<StatisticsGlobal> getGlobalStatisticsWithHttpInfo(String group, List<String> subGroup) throws ApiException {
        com.squareup.okhttp.Call call = getGlobalStatisticsValidateBeforeCall(group, subGroup, null, null);
        Type localVarReturnType = new TypeToken<StatisticsGlobal>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Get stats. (asynchronously)
     * Get global statistics for this repository.
     * @param group primary property to build facettes and count+group values (optional)
     * @param subGroup additional properties to build facettes and count+sub-group values (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getGlobalStatisticsAsync(String group, List<String> subGroup, final ApiCallback<StatisticsGlobal> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getGlobalStatisticsValidateBeforeCall(group, subGroup, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<StatisticsGlobal>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getStatisticsNode
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getStatisticsNodeCall(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/statistic/v1/statistics/nodes";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (grouping != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("grouping", grouping));
        if (dateFrom != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("dateFrom", dateFrom));
        if (dateTo != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("dateTo", dateTo));
        if (mediacenter != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("mediacenter", mediacenter));
        if (additionalFields != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("multi", "additionalFields", additionalFields));
        if (groupField != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("multi", "groupField", groupField));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getStatisticsNodeValidateBeforeCall(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'grouping' is set
        if (grouping == null) {
            throw new ApiException("Missing the required parameter 'grouping' when calling getStatisticsNode(Async)");
        }
        
        // verify the required parameter 'dateFrom' is set
        if (dateFrom == null) {
            throw new ApiException("Missing the required parameter 'dateFrom' when calling getStatisticsNode(Async)");
        }
        
        // verify the required parameter 'dateTo' is set
        if (dateTo == null) {
            throw new ApiException("Missing the required parameter 'dateTo' when calling getStatisticsNode(Async)");
        }
        

        com.squareup.okhttp.Call call = getStatisticsNodeCall(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * get statistics for node actions
     * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS for global stats or to be admin of the requested mediacenter
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @return List&lt;TrackingNode&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public List<TrackingNode> getStatisticsNode(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body) throws ApiException {
        ApiResponse<List<TrackingNode>> resp = getStatisticsNodeWithHttpInfo(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body);
        return resp.getData();
    }

    /**
     * get statistics for node actions
     * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS for global stats or to be admin of the requested mediacenter
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @return ApiResponse&lt;List&lt;TrackingNode&gt;&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<List<TrackingNode>> getStatisticsNodeWithHttpInfo(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body) throws ApiException {
        com.squareup.okhttp.Call call = getStatisticsNodeValidateBeforeCall(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body, null, null);
        Type localVarReturnType = new TypeToken<List<TrackingNode>>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * get statistics for node actions (asynchronously)
     * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS for global stats or to be admin of the requested mediacenter
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getStatisticsNodeAsync(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body, final ApiCallback<List<TrackingNode>> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getStatisticsNodeValidateBeforeCall(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<List<TrackingNode>>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getStatisticsUser
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getStatisticsUserCall(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = body;

        // create path and map variables
        String localVarPath = "/statistic/v1/statistics/users";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (grouping != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("grouping", grouping));
        if (dateFrom != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("dateFrom", dateFrom));
        if (dateTo != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("dateTo", dateTo));
        if (mediacenter != null)
        localVarQueryParams.addAll(apiClient.parameterToPair("mediacenter", mediacenter));
        if (additionalFields != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("multi", "additionalFields", additionalFields));
        if (groupField != null)
        localVarCollectionQueryParams.addAll(apiClient.parameterToPairs("multi", "groupField", groupField));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getStatisticsUserValidateBeforeCall(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        // verify the required parameter 'grouping' is set
        if (grouping == null) {
            throw new ApiException("Missing the required parameter 'grouping' when calling getStatisticsUser(Async)");
        }
        
        // verify the required parameter 'dateFrom' is set
        if (dateFrom == null) {
            throw new ApiException("Missing the required parameter 'dateFrom' when calling getStatisticsUser(Async)");
        }
        
        // verify the required parameter 'dateTo' is set
        if (dateTo == null) {
            throw new ApiException("Missing the required parameter 'dateTo' when calling getStatisticsUser(Async)");
        }
        

        com.squareup.okhttp.Call call = getStatisticsUserCall(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body, progressListener, progressRequestListener);
        return call;

    }

    /**
     * get statistics for user actions (login, logout)
     * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS for global stats or to be admin of the requested mediacenter
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @return List&lt;Tracking&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public List<Tracking> getStatisticsUser(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body) throws ApiException {
        ApiResponse<List<Tracking>> resp = getStatisticsUserWithHttpInfo(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body);
        return resp.getData();
    }

    /**
     * get statistics for user actions (login, logout)
     * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS for global stats or to be admin of the requested mediacenter
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @return ApiResponse&lt;List&lt;Tracking&gt;&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<List<Tracking>> getStatisticsUserWithHttpInfo(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body) throws ApiException {
        com.squareup.okhttp.Call call = getStatisticsUserValidateBeforeCall(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body, null, null);
        Type localVarReturnType = new TypeToken<List<Tracking>>(){}.getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * get statistics for user actions (login, logout) (asynchronously)
     * requires either toolpermission TOOLPERMISSION_GLOBAL_STATISTICS for global stats or to be admin of the requested mediacenter
     * @param grouping Grouping type (by date) (required)
     * @param dateFrom date range from (required)
     * @param dateTo date range to (required)
     * @param mediacenter the mediacenter to filter for statistics (optional)
     * @param additionalFields additionals fields of the custom json object stored in each query that should be returned (optional)
     * @param groupField grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date) (optional)
     * @param body filters for the custom json object stored in each entry (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getStatisticsUserAsync(String grouping, Long dateFrom, Long dateTo, String mediacenter, List<String> additionalFields, List<String> groupField, Object body, final ApiCallback<List<Tracking>> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = getStatisticsUserValidateBeforeCall(grouping, dateFrom, dateTo, mediacenter, additionalFields, groupField, body, progressListener, progressRequestListener);
        Type localVarReturnType = new TypeToken<List<Tracking>>(){}.getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
