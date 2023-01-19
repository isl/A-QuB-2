/**
 * The main angularJS controllers (handling tabs, the query submission and serverside paginator and the file importing
 * 
 */
app.controller("welcomeCtrl", ['$state', '$scope', '$sessionStorage', 'authenticationService', '$mdSidenav', function ($state, $scope, $sessionStorage, authenticationService, $mdSidenav) {
//app.controller("welcomeCtrl", ['$transitions', '$state', '$scope', '$sessionStorage', function($transitions, $state, $scope, $sessionStorage) {
        /*
         // Using session to fix the browser refresh page issue
         $transitions.onSuccess({to: true}, function ($state) {
         //console.log("$state.data.selectedTab: " + $state.router.globals.current.data.selectedTab)
         if($state.router.globals.current.data != undefined)
         $sessionStorage.currentTab = $state.router.globals.current.data.selectedTab;
         });
         */

        $scope.headingTitle = "Home";

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

        // Used to inform user that his token is no longer valid and will be logged out
        $scope.showLogoutAlert = function () {
            $mdDialog.show(
                    $mdDialog.alert()
                    .parent(angular.element(document.querySelector('#popupContainer')))
                    .clickOutsideToClose(true)
                    .title('Attention Please')
                    .textContent('Either your session has been expired or you are no longer authorized to continue.')
                    .ariaLabel('Alert Dialog Demo')
                    .ok('OK')
                    ).finally(function () {
                $state.go('login', {});
            });
        };

        // Used to inform user that error has occured
        $scope.showErrorAlert = function (title, msg) {
            $mdDialog.show(
                    $mdDialog.alert()
                    .parent(angular.element(document.querySelector('#popupContainer')))
                    .clickOutsideToClose(true)
                    .title(title)
                    .textContent(msg)
                    .ariaLabel('Alert Dialog')
                    .ok('OK')
                    )
        };

        // Regarding roles

        $scope.hasRoleOfAdministrator = false;
        $scope.hasRoleOfResearcher = false;
        $scope.hasRoleOfOperator = false;
        $scope.hasRoleOfController = false;

        $scope.userProfile = authenticationService.getUserProfile();

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

        $scope.cards = [{
                index: "0",
                title: "Explore Data",
                description: "Data navigation through a simple and user friendly interface. ",
                icon: "mdi-magnify",
                size: "150px",
                actionButtonLabel: "Continue",
                href: "",
                state: "navigation",
                disabled: false,
                view: ""
            }, {
                index: "1",
                title: "Import Data",
                description: "An easy to use tool for data import, organized into collections. ",
                icon: "mdi-database-plus",
                size: "150px",
                actionButtonLabel: "Continue",
                href: "",
                state: "import",
                disabled: !($scope.hasRoleOfAdministrator || $scope.hasRoleOfOperator),
                view: ""
            }, {
                index: "2",
                title: "My Favorites",
                description: "Data navigation might require a few steps to be achieved. However, as soon as important findings are achieved, they can be stored for direct feature access.",
                icon: "mdi-star",
                size: "150px",
                actionButtonLabel: "Continue",
                href: "",
                state: "favorites",
                disabled: !$scope.hasRoleOfAdministrator && !$scope.hasRoleOfResearcher && !$scope.hasRoleOfOperator,
                view: ""
            }
        ];

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

    }]);