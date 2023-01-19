app.controller("privacyPolicyCtrl", ['$state', '$scope', '$timeout', '$parse', '$sessionStorage', 'authenticationService', 'modalService', 'queryService', '$mdSidenav', '$mdToast', '$http', '$log', '$mdDialog', '$mdToast',
    function ($state, $scope, $timeout, $parse, $sessionStorage, authenticationService, modalService, queryService, $mdSidenav, $mdToast, $http, $log, $mdDialog, $mdToast) {

        var baseRootPath = '/aqub';

        $scope.headingTitle = "Disclaimer";

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

        $scope.acceptPrivacyPolicy = function () {
            console.log('Accept Privacy Policy');
        }

        $scope.declinePrivacyPolicy = function () {
            console.log('Decline Privacy Policy');
        }

    }]);