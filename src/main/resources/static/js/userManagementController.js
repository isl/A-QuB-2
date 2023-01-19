app.controller("userManagementCtrl", ['$state', '$scope', '$timeout', '$parse', '$sessionStorage', 'authenticationService', 'modalService', 'queryService', '$mdSidenav', '$mdToast', '$http', '$log', '$mdDialog', '$mdToast',
    function ($state, $scope, $timeout, $parse, $sessionStorage, authenticationService, modalService, queryService, $mdSidenav, $mdToast, $http, $log, $mdDialog, $mdToast) {

        var baseRootPath = '/aqub';

        $scope.headingTitle = "User Management";

        // Calling service to get the user's credentials (token, userId)
        function initCredentials() {
            $scope.credentials = authenticationService.getCredentials();
            if ($scope.credentials == undefined) {
                $state.go('login', {});
            }
        }

        initCredentials();

        $scope.currUserProfile = authenticationService.getUserProfile();

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

        $scope.roles = [
            {label: "Researcher", value: "RESEARCHER"},
            {label: "Operator", value: "OPERATOR"},
            {label: "Administrator", value: "ADMIN"},
            {label: "Controller", value: "CONTROLLER"},
            {label: "Demo", value: "DEMO"}
        ];

        // Array to hold all users
        $scope.allUserProfiles = [];

        // Retrieving the list of user
        function retrieveAllUsers() {

            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Retrieving data...'
            };
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            authenticationService.retrieveAllUserProfiles($scope.credentials.token)
                    .then(function (response) {

                        if (response.status == '200') {
                            console.log(response.data.response);
                            console.log(response.data.response.length)
                            if (response.data.response.length > 0) {
                                console.log("response", response.data);
                                $scope.allUserProfiles = response.data.response

                            } else {
                                $scope.alerts.push({type: 'danger-funky', msg: profileResponse.data.message + "! There are no users available!"});
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

        // Get user's profile from session
        retrieveAllUsers();

        // Selecting the model to load
        $scope.setSelectedUserProfile = function () {
            $scope.selectedUserProfile = this.item; // this.item is from the ng-repeat in html
            $scope.selectedUserProfileBackup = this.item; // to be used at cancel
            console.log("selectedUserProfile", $scope.selectedUserProfile);
        };

        // Updates the profile of the selected user
        $scope.updateUserProfile = function () {

            // Pre-processing
            var tempUserProfile = angular.copy($scope.selectedUserProfile);

            tempUserProfile.userid = tempUserProfile.userId;
            delete tempUserProfile.userId;

            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Updating user\'s profile...'
            };
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            authenticationService.updateProfile($scope.credentials.token, tempUserProfile)
                    .then(function (response) {

                        if (response.status == '200') {
                            console.log("Profile response: " + response.data);
                            // Prompt success message
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
                        //modalInstance.close();
                        $scope.message = 'There was a network error. Try again later.';
                        $scope.showErrorAlert('Error', $scope.message + "\n" + error);
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

        // Regarding user's roles and whether they should be editable

        $scope.portalPolicy = {};

        // Calling service to retrieve the portal's state (public or private)
        // For "public" state, user's role editinf should be disabled
        function retrieveState() {

            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Retrieving data...'
            };
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            authenticationService.getPortalState()
                    .then(function (response) {

                        if (response.status == -1) {
                            $scope.message = 'There was a network error. Try again later.';
                            $scope.showErrorAlert('Error', $scope.message);
                        } else {
                            if (response.status == '200') {
                                $scope.portalPolicy = response.data;
                            } else if (response.status == '408') {
                                $log.info(response.status);
                                $scope.message = 'It seems that it takes a lot of time to complete this task! Please redifine your query and try again.';
                                $scope.showErrorAlert('Important', $scope.message);
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

                        } // else close

                    }, function (error) {
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

        // Call Method to retrieve portal's state
        retrieveState();

    }]);