angular.module('app.mainServices', [])

        .factory('queryService', function ($http, $timeout, $q) {

            var splitStr = '#@#';
            var baseRootPath = '/aqub';

            return {
                computeRelatedEntityQuery: function (searchEntityModel, token) {
                    console.log('====>computeRelatedEntityQuery<====')
                    return $http({
                        'url': baseRootPath + '/related_entity_query',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': searchEntityModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });
                },
                getEntityQueryResultsCount: function (serviceModel, queryModel, token) {
                    console.log('====>getEntityQueryResultsCount<====')
                    return $http({
                        'url': serviceModel.url + '/query/virtuoso/count',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': queryModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });

                },
                getEntityQueryResults: function (serviceModel, queryModel, token) {
                    console.log('====>getEntityQueryResults<====')
                    return $http({
                        'url': serviceModel.url + '/query/virtuoso',
                        'method': 'POST',
                        'headers': {
                            'Access-Control-Allow-Origin': '*',
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': queryModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        console.log(error);
                        return error;
                    });
                },
                getEntities: function (queryFrom, token) {
                    console.log('====>getEntities<====')
                    return $http({
                        'url': baseRootPath + '/get_entities',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': {fromSearch: queryFrom}
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });
                },
                /*
                 getAllEntities : function() {
                 return $http({
                 'url' : '/get_all_entities',
                 'method' : 'GET',
                 'headers' : {
                 'Content-Type' : 'application/json'
                 },
                 'params' : ''
                 }).then(function (success){
                 return success;
                 },function (error) {
                 //error code
                 });
                 },
                 */
                getAllNamegraphs: function () {
                    return $http({
                        'url': baseRootPath + '/get_all_namedgraphs',
                        'method': 'GET',
                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'params': ''
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });
                },
                getDefaultNamegraphs: function () {
                    return $http({
                        'url': baseRootPath + '/get_default_namedgraphs',
                        'method': 'GET',
                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'params': ''
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });
                },
                getRelationsAndRelatedEntitiesByTarget: function (paramModel, token) {
                    return $http({
                        //'url': '/get_relations_related_entities',
                        'url': baseRootPath + '/dynamic/get_relations_related_entities',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': paramModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });
                },
                getRelationsByTargetAndRelatedEntity: function (paramModel, token) {
                    return $http({
                        //'url': '/get_relations',
                        'url': baseRootPath + '/dynamic/get_relations',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': paramModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });
                },
                computeFinalSearchQuery: function (paramModel, token) {
                    return $http({
                        'url': baseRootPath + '/final_search_query',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': paramModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });
                },
                getFinalQueryResults: function (paramModel, token) {
                    return $http({
                        'url': baseRootPath + '/execute_final_query',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': paramModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });
                },
                getFinalQueryResultsForPage: function (paramModel, token) {
                    return $http({
                        'url': baseRootPath + '/get_final_query_results_per_page',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': paramModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });
                },
                retrieveEntityInfo: function (paramModel, token) {
                    return $http({
                        'url': baseRootPath + '/retrieve_entity_info',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': paramModel
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        return error;
                    });
                },
                checkAuthorization: function (token) {
                    return $http({
                        'url': baseRootPath + '/checkAuthorization',
                        'method': 'GET',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        }
                    }).then(function (success) {
                        return success.data;
                    }, function (error) {
                        //error code
                    });

                },
                getGeoQueryResults: function (queryModel, token) {

                    return $http({
                        'url': baseRootPath + '/executegeoquery_json',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': queryModel
                    }).then(function (success) {
                        if (success.data.result != null) {
                            combineValuesToCreateLinks(success.data.result.results, splitStr);
                        }
                        return success.data;
                    }, function (error) {
                        //error code
                    });

                },
                saveIntoFavorites: function (model, token) {

                    return $http({
                        'url': baseRootPath + '/save_into_favorites',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': model
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                },
                removeFromFavoritesById: function (model, token) {

                    return $http({
                        'url': baseRootPath + '/remove_from_favorites_by_id',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': model
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                },
                retrieveFavoriteQueryModelsByUsername: function (model, token) {

                    return $http({
                        'url': baseRootPath + '/retrieve_favorite_query_models_by_user',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': model
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                },
                retrieveserviceModel: function (token) {
                    console.log('====>retrieveserviceModel<====')
                    return $http({
                        'url': baseRootPath + '/retrieve_service_model',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        }
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                }//,

            }

        })

        .factory('importService', function ($http, $timeout, $q) {


            // TODO: Define importModel in the controller
            return {
                /*
                 importRdf : function(importModel, token) {
                 
                 return $http({
                 'url' : '/impor_rdf',
                 'method' : 'POST',
                 'headers' : {
                 'Content-Type' : 'application/json',
                 'Authorization': token
                 },
                 'data' : importModel
                 }).then(function (success){
                 return success;
                 },function (error) {
                 return error;
                 });
                 
                 },
                 */
                createMetadataInfo: function (model, token) {

                    return $http({
                        'url': baseRootPath + '/createGraphMetadata',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': model
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                },
                insertUserProfileMetadataInfo: function (model, token) {

                    return $http({
                        'url': baseRootPath + '/insertUserProfileMetadata',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': model
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                },
                materializeNewData: function (model, token) {

                    return $http({
                        'url': baseRootPath + '/after_upload_process',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json',
                            'Authorization': token
                        },
                        'data': model
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                }

            }
        });