
app.directive('compareTo', function () {
    return {
        require: "ngModel",
        scope: {
            otherModelValue: "=compareTo"
        },
        link: function (scope, element, attributes, ngModel) {
            ngModel.$validators.compareTo = function (modelValue) {
                return modelValue == scope.otherModelValue;
            };

            scope.$watch("otherModelValue", function () {
                ngModel.$validate();
            });
        }
    };
});

app.directive('greaterThan', function () {
    return {
        require: "ngModel",
        scope: {
            otherModelValue: "=greaterThan"
        },
        link: function (scope, element, attributes, ngModel) {
            ngModel.$validators.greaterThan = function (modelValue) {
                console.log(modelValue);
                return (modelValue >= scope.otherModelValue) || modelValue == null;
            };

            scope.$watch("otherModelValue", function () {
                ngModel.$validate();
            });
        }
    };
});

app.controller("loginCtrl", ['$scope', 'authenticationService', '$location', '$timeout', '$state', '$mdDialog', '$sessionStorage',
    function ($scope, authenticationService, $location, $timeout, $state, $mdDialog, $sessionStorage) {

        $sessionStorage.$reset();
        $scope.headingTitle = "Login";

        // Alert (danger, warning, success)
        $scope.alerts = [];

        $scope.credentials = {};

        $scope.tempMFAToken = {};

        // Usd for Multifactor Authentication checkbox
        $scope.multifactorAuthenticatorActive = false;

        $scope.goToRegistration = function () {
            $state.go('registration', {});
        }

        $scope.login = function (ev) {
            //alert("username is: " + $scope.credentials.username + "and password is: " + $scope.credentials.password);
            $scope.credentials.dataLoading = true;

            // Regular Authentication
            if ($scope.multifactorAuthenticatorActive == false) {
                /*
                 authenticationService.retrieveUserProfile('', 'SomeUsername')
                 $state.go('welcome', {});
                 */
                authenticationService.login($scope.credentials.username, $scope.credentials.password)
                        .then(function (response) {
                            if (response.status == 'SUCCEED') {
                                // Redirect to the tabs.queryTab
                                console.log("response", response.token);
                                // Retrieve User's profile
                                authenticationService.retrieveUserProfile(response.token, $scope.credentials.username)
                                        .then(function (profileResponse) {
                                            if (profileResponse.data.name !== null) {
                                                console.log("profileResponse", profileResponse.data);
                                                $sessionStorage.userProfile = profileResponse.data;
                                                $state.go('welcome', {});
                                            } else {
                                                $scope.alerts.push({type: 'danger-funky', msg: profileResponse.data.message + "! Cannot retrieve user's profile. "});
                                            }
                                        }, function (error) {
                                            $scope.message = 'There was a network error. Try again later.';
                                            alert("failure message: There was a network error. Try again later");
                                        });

                            } else {
                                $scope.alerts.splice(0); // Close alerts
                                $scope.alerts.push({type: 'danger-funky', msg: response.message + "! Authendication failed, please check your credentials and try again. "});
                                $scope.credentials.dataLoading = false;

                            }
                        }, function (error) {
                            $scope.message = 'There was a network error. Please try again later.';
                            alert("There was a network error. Please try again later");
                        });

            }

            // Multifactor Authentication
            else {
                authenticationService.loginMFA($scope.credentials.username, $scope.credentials.password)
                        .then(function (response) {
                            if (response.status == 'SUCCEED') {
                                //$scope.tempMFAToken = response.token;
                                $scope.tempMFAToken = response.token;
                                console.log("response", response.token);
                                $scope.showMFACodePrompt(ev);
                            } else {
                                $scope.alerts.splice(0); // Close alerts
                                $scope.alerts.push({type: 'danger-funky', msg: response.message + "! Authendication failed, please check your credentials and try again. "});
                                $scope.credentials.dataLoading = false;

                            }
                        }, function (error) {
                            $scope.message = 'There was a network error. Please try again later.';
                            alert("There was a network error. Please try again later");
                        });

            }

        }

        $scope.showMFACodePrompt = function (ev) {

            // Appending dialog to document.body to cover sidenav in docs app
            var htmlContent = 'In a few seconds you will receive some <code style="color:#106CC8;background: rgba(0,0,0,0.065);">code</code> on your mobile phone through the Telegram application.'
            $mdDialog.show({
                scope: $scope,
                preserveScope: true,
                controller: 'mfaDialogController',
                templateUrl: 'views/dialog/mfaDialog.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: ev
            });

        };

    }]);

app.controller("mfaDialogController", ['$scope', 'authenticationService', '$mdDialog', '$state', '$sessionStorage',
    function ($scope, authenticationService, $mdDialog, $state, $sessionStorage) {

        $scope.dialogAlerts = [];

        $scope.mfaCode = "";

        $scope.tempMFAToken;

        $scope.proceedWidthMFACode = function () {

            console.log("$scope.tempMFAToken: " + $scope.tempMFAToken);

            authenticationService.loginMFACode($scope.tempMFAToken, $scope.mfaCode)
                    .then(function (response) {
                        if (response.status == 'SUCCEED') {
                            console.log("response", response.token);
                            // Retrieve User's profile
                            authenticationService.retrieveUserProfile(response.token, $scope.credentials.username)
                                    .then(function (profileResponse) {
                                        if (profileResponse.data.name !== null) {
                                            console.log("profileResponse", profileResponse.data);
                                            $sessionStorage.userProfile = profileResponse.data;
                                            $state.go('welcome', {});
                                        } else {
                                            $scope.dialogAlerts.push({type: 'danger-funky', msg: profileResponse.data.message + "! Cannot retrieve user's profile. "});
                                        }
                                    }, function (error) {
                                        $scope.message = 'There was a network error. Try again later.';
                                        alert("failure message: There was a network error. Try again later");
                                    });

                        } else {
                            $scope.dialogAlerts.splice(0); // Close alerts
                            $scope.dialogAlerts.push({type: 'danger-funky', msg: response.message + "! Authendication failed, please check your credentials and try again. "});
                            $scope.credentials.dataLoading = false;
                        }
                    }, function (error) {
                        $scope.message = 'There was a network error. Please try again later.';
                        alert("There was a network error. Please try again later");
                    });

        }

        $scope.cancelMdfCode = function () {
            $mdDialog.hide();
        }

    }]);


app.controller("beforeLoginCtrl", ['$scope', 'authenticationService', 'homeStateConfirmService', '$location', '$timeout', '$state', '$mdDialog',
    function ($scope, authenticationService, homeStateConfirmService, $location, $timeout, $state, $mdDialog) {
        $scope.headingTitle = "Login";
        $scope.userProfile = {};

        // Calling service to get the user's profile
        $scope.updateUserProfile = function () {

            $scope.userProfile = authenticationService.getUserProfile();

            // Regarding roles

            $scope.hasRoleOfAdministrator = false;
            $scope.hasRoleOfResearcher = false;
            $scope.hasRoleOfOperator = false;
            $scope.hasRoleOfController = false;

            $scope.hasRole = function (role) {
                if ($scope.userProfile != null) {
                    if ($scope.userProfile.role != null) {
                        if ($scope.userProfile.role == role)
                            return true;
                        else
                            return false;
                    } else
                        return false;
                } else
                    return false;
            }

            $scope.hasRoleOfAdministrator = $scope.hasRole('ADMIN');
            $scope.hasRoleOfResearcher = $scope.hasRole('RESEARCHER');
            $scope.hasRoleOfOperator = $scope.hasRole('OPERATOR');
            $scope.hasRoleOfController = $scope.hasRole('CONTROLLER');
        }

        $scope.$watch($scope.updateUserProfile, function () {
        });

        $scope.sessionAuthenticatedStatus = function () {
            return authenticationService.isAuthenticated();
        };

        $scope.currentState = function () {
            return $state.current.name;
        };
        $scope.logout = function () {
            authenticationService.clearCredentials();
            //  authenticationService.logout(authenticationService.getCredentials().token)
            $state.go("welcome",{},{reload: "welcome"});
        }

        $scope.openTopMenu = function ($mdMenu, ev) {
            originatorEv = ev;
            $mdMenu.open(ev);
        };

        $scope.goToHomeView = function (ev) {
            // Checks if there is any query currently under construction
            // and prompts message or just navigate to the new page
            if (homeStateConfirmService.isQueryUnderConstruction())
                confirmLeavingFromQueryBuilder(ev, 'welcome');
            else
                $state.go('welcome', {});
        }

        $scope.goToLoginView = function (ev) {
            // Checks if there is any query currently under construction
            // and prompts message or just navigate to the new page
            if (homeStateConfirmService.isQueryUnderConstruction())
                confirmLeavingFromQueryBuilder(ev, 'login');
            else
                $state.go('login', {});
        }
        $scope.goToFavoritesView = function (ev) {
            // Checks if there is any query currently under construction
            // and prompts message or just navigate to the new page
            if (homeStateConfirmService.isQueryUnderConstruction())
                confirmLeavingFromQueryBuilder(ev, 'favorites');
            else
                $state.go('favorites', {});
        }

        // Navigates to user management section
        $scope.goToUserManagement = function (ev) {
            // Checks if there is any query currently under construction
            // and prompts message or just navigate to the new page
            if (homeStateConfirmService.isQueryUnderConstruction())
                confirmLeavingFromQueryBuilder(ev, 'userManagement');
            else
                $state.go('userManagement', {});
        }

        $scope.goToUserProfileView = function (ev) {
            // Checks if there is any query currently under construction
            // and prompts message or just navigate to the new page
            if (homeStateConfirmService.isQueryUnderConstruction())
                confirmLeavingFromQueryBuilder(ev, 'userProfile');
            else
                $state.go('userProfile', {});
        }

        $scope.goToPrivacyPolicyView = function (ev) {
            $state.go('privacyPolicy', {});
        }

        // Ask confirmation before leaving query builder if you are there
        function confirmLeavingFromQueryBuilder(ev, state) {
            var confirm = $mdDialog.confirm()
                    .title('Warning Message')
                    .htmlContent('It seems that some query is under construction. </br>Are you sure you want to leave this page?')
                    .ariaLabel('Confirmation')
                    .targetEvent(ev)
                    .ok('Yes I want to leave')
                    .cancel('No, stay here');

            $mdDialog.show(confirm).then(function () { // OK
                homeStateConfirmService.setQueryUnderConstruction(false);
                $state.go(state, {});
            }, function () { // Cancel
                // do nothing
            });
        }

    }]);

app.controller("registrationCtrl", ['$scope', 'authenticationService', 'modalService', '$timeout', '$state',
    function ($scope, authenticationService, modalService, $timeout, $state) {
        var baseRootPath = '/aqub';

        $scope.headingTitle = "Registration Form";

        var modalDefaults = {
            backdrop: true,
            keyboard: true,
            modalFade: true,
            templateUrl: baseRootPath + '/views/loadingModal.html'
        };

        $scope.roles = [
            {label: "Researcher", value: "RESEARCHER"},
            {label: "Operator", value: "OPERATOR"},
            {label: "Administrator", value: "ADMIN"},
            {label: "Controller", value: "CONTROLLER"},
        ];

        $scope.registration = {};

        // Alert (danger, warning, success)
        $scope.alerts = [];

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

        $scope.register = function () {

            // Before applying registration, check there is any role selected 
            // and if there is not then autoselect researcher
            if ($scope.registration.role == null)
                $scope.registration.role = "RESEARCHER";

            // Register
            authenticationService.register($scope.registration)
                    .then(function (response) {
                        if (response.status == 'SUCCEED') {
                            $timeout(function () {
                                // Redirect to the tabs.queryTab
                                $state.go('login', {});
                                $scope.alerts.splice(0); // Close alerts
                                $scope.message = 'User ' + $scope.registration.userid + ' was successfully registered!';
                            }, 2);
                        } else {
                            $scope.alerts.push({type: 'danger-funky', msg: response.message});
                        }
                    }, function (error) {
                        $scope.alerts.splice(0); // Close alerts
                        $scope.message = 'There was a network error. Please try again later.';
                        //$scope.alerts.push({type: 'danger-funky', msg: message});
                        alert("There was a network error. Please try again later");
                    });

        }

        $scope.goToLogin = function () {
            $state.go('login', {});
        }

    }]);
