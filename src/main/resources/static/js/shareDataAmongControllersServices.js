angular.module('app.shareDataAmongControllersServices', [])

.factory('homeStateConfirmService', function($http, $timeout, $q) {
	
	// To hold the currentUser
	var queryUnderConstruction = false;

	return {
        isQueryUnderConstruction: function () {
            return queryUnderConstruction;
        },
        setQueryUnderConstruction: function (someBoolean) {
        	queryUnderConstruction = someBoolean;
        }
    };
    
});