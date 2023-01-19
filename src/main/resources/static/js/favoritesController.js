app.controller("favoritesCtrl", ['$state', '$scope', '$timeout', '$parse', '$sessionStorage', 'authenticationService', 'modalService', 'queryService', '$mdSidenav', '$mdToast', '$http', '$log', '$mdDialog', '$mdToast',
    function ($state, $scope, $timeout, $parse, $sessionStorage, authenticationService, modalService, queryService, $mdSidenav, $mdToast, $http, $log, $mdDialog, $mdToast) {

        var baseRootPath = '/aqub';

        $scope.headingTitle = "My Favorites";

        // Calling service to get the user's credentials (token, userId)
        function initCredentials() {
            $scope.credentials = authenticationService.getCredentials();
            if ($scope.credentials == undefined) {
                $state.go('login', {});
            }
        }

        initCredentials();

        function checkAuthorization() {

        }

        checkAuthorization();

        var modalDefaultOptions = {
            headerText: 'Loading Please Wait...',
            bodyText: 'Your query is under process...'
        };

        var modalDefaults = {
            backdrop: true,
            keyboard: true,
            modalFade: true,
            templateUrl: baseRootPath + '/views/loadingModal.html'
        };

        // Used to inform user that his token is no longer valid and will be logged out
        $scope.showLogoutAlert = function () {
            // Appending dialog to document.body to cover sidenav in docs app
            // Modal dialogs should fully cover application
            // to prevent interaction outside of dialog
            $mdDialog.show(
                    $mdDialog.alert()
                    .parent(angular.element(document.querySelector('#popupContainer')))
                    .clickOutsideToClose(true)
                    .title('Attention Please')
                    .textContent('Either your session has been expired or you are no longer authorized to continue.')
                    .ariaLabel('Logout Message')
                    .ok('OK')
                    ).finally(function () {
                $state.go('login', {});
            });
        };

        // Used to inform user that error has occurred
        $scope.showErrorAlert = function (title, msg) {
            $mdDialog.show(
                    $mdDialog.alert()
                    .parent(angular.element(document.querySelector('#popupContainer')))
                    .clickOutsideToClose(true)
                    .title(title)
                    .textContent(msg)
                    .ariaLabel('Error Message')
                    .ok('OK')
                    )
        };

        $scope.goToHomeView = function () {
            $state.go('welcome', {});
        }

        $scope.goToState = function (stateName) {
            $state.go(stateName, {});
        }

        // Toggles SidePanel
        $scope.toggleInfo = buildToggler('rightInfo');

        function buildToggler(componentId) {
            return function () {
                $mdSidenav(componentId).toggle();
            };
        }

        $scope.currentFavorite = {};
        $scope.favoriteModels = [];

        // Init favorites
        function initFavoriteModels() {
            retrieveUserFavorites($sessionStorage.userProfile.userId);
        }
        initFavoriteModels();

        // Retrieving the list of favoriteModels of the user
        function retrieveUserFavorites(username) {

            var model = {username: username};

            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Retrieving data...'
            };
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            queryService.retrieveFavoriteQueryModelsByUsername(model, $scope.credentials.token)
                    .then(function (response) {

                        if (response.status == '200') {
                            if (response.data.dbStatus == 'success') {
                                if (response.data.favoriteModels != null)
                                    $scope.favoriteModels = response.data.favoriteModels;
                            } else {
                                $scope.message = 'I\'m sorry! Something went wrong and your list of favorites could not be loaded. Try again later and if the same error occures, please contact with the administrator.';
                                $scope.showErrorAlert('Error', $scope.message);
                            }
                        } else if (response.status == '400') {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                        } else if (response.status == '401') {
                            $log.info(response.status);
                            $scope.showLogoutAlert();
                            authenticationService.clearCredentials();
                        } else {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                        }
                        //modalInstance.close();

                    }, function (error) {
                        //modalInstance.close();
                        $scope.message = 'There was a network error. Try again later.';
                        alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                            data: error
                        }));
                    }).finally(function () {
                $timeout(function () {
                    modalInstance.close();
                });//, 500);
            });

        }

        // Converting object to array in order to use orderBy filter on ng-repeat
        // To bypass the error "Error: 10 $digest() iterations reached. Aborting!"
        // the results of the function are cashed by using the Lo-Dash’s memoize function
        $scope.templateAry = _.memoize(function (tempItem) {
            var ary = [];
            angular.forEach(tempItem, function (val, key) {
                if (key != '$$hashKey' && key != 'queryModel' && key != 'favoriteId' && key != 'username') // $$hashKey is added by angular (don't need it)
                    ary.push({key: key, val: val});
            });
            return ary;
        });

        // Selecting the model to load
        $scope.setSelectedFavoriteModel = function () {
            //$scope.selectedQueryModel = this.item.queryModel; // this.item is from the ng-repeat in html
            $scope.selectedFavoriteModel = this.item; // this.item is from the ng-repeat in html
        };

        // Loading model
        $scope.loadQueryModel = function () {
            //console.log($scope.selectedQueryModel);
            //$sessionStorage.selectedQueryModel = angular.fromJson($scope.selectedQueryModel);
            console.log($scope.selectedFavoriteModel);
            $sessionStorage.selectedFavoriteModel = angular.fromJson($scope.selectedFavoriteModel);
            $state.go('navigation', {});
        }

        $scope.deleteQueryModel = function (ev) {

            var confirm = $mdDialog.confirm({
                onComplete: function afterShowAnimation() {
                    var $dialog = angular.element(document.querySelector('md-dialog'));
                    var $actionsSection = $dialog.find('md-dialog-actions');
                    //$actionsSection.css("text-align", "center");
                    //$actionsSection.css("display", "block");
                    var $cancelButton = $actionsSection.children()[0];
                    var $confirmButton = $actionsSection.children()[1];
                    angular.element($confirmButton).addClass('md-raised md-warn');
                    angular.element($cancelButton).addClass('md-raised');
                }
            })
                    .title('Warning Message')
                    .htmlContent("Are you sure you want to <code>delete</code> the selected query model.</br> Please note that <code>fulfilling this action, leaves no recovering options</code>.")
                    .ariaLabel('Query Model deletion')
                    .targetEvent(ev)
                    .ok('Yes Continue')
                    .cancel('Cancel');

            $mdDialog.show(confirm).then(function () { // OK

                $scope.currentFavorite.dbTableId = $scope.selectedFavoriteModel.favoriteId;
                queryService.removeFromFavoritesById($scope.currentFavorite, $scope.credentials.token)
                        .then(function (response) {
                            if (response.status == '200') {
                                if (response.data.dbStatus == 'success') {

                                    // Re-retrieve favorites
                                    initFavoriteModels();

                                    // Display msg
                                    $mdToast.show(
                                            $mdToast.simple()
                                            .textContent('The Query has been removed from your favorites!')
                                            .position('top right')
                                            .parent(angular.element('#mainContent'))
                                            .hideDelay(3000)
                                            );
                                } else {
                                    $scope.message = 'I\'m sorry! The query was able to be stored. Try again later and if the same error occures, please contact with the administrator.';
                                    $scope.showErrorAlert('Error', $scope.message);
                                }
                            } else if (response.status == '400') {
                                $log.info(response.status);
                                $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                $scope.showErrorAlert('Error', $scope.message);
                            } else if (response.status == '401') {
                                $log.info(response.status);
                                $scope.showLogoutAlert();
                                authenticationService.clearCredentials();
                            } else {
                                $log.info(response.status);
                                $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                $scope.showErrorAlert('Error', $scope.message);
                            }

                        }, function (error) {
                            $scope.message = 'There was a network error. Try again later.';
                            alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                                data: error
                            }));
                        });

            }, function () { // Cancel
                // Do nothing
            });

        }

    }]);