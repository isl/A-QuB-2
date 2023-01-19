app.controller("navigationCtrl", ['$state', '$scope', '$timeout', '$parse', '$sessionStorage', 'authenticationService', 'modalService', 'queryService', 'homeStateConfirmService', '$mdSidenav', '$window', 'ivhTreeviewMgr', '$http', '$log', '$mdDialog', '$mdToast', '$q', '$filter',
    function ($state, $scope, $timeout, $parse, $sessionStorage, authenticationService, modalService, queryService, homeStateConfirmService, $mdSidenav, $window, ivhTreeviewMgr, $http, $log, $mdDialog, $mdToast, $q, $filter) {

        var baseRootPath = '/aqub';
        $scope.headingTitle = "Explore Data";
        $scope.favoriteTitle = '';

        // Calling service to get the user's credentials (token, userId)
        function initCredentials() {
            $scope.credentials = authenticationService.getCredentials();
            if ($scope.credentials == undefined) {
                $scope.credentials = {token: null};
                $state.go('login', {});
            }
        }

        initCredentials();

        // Regarding user roles
        $scope.hasRoleOfAdministrator = false;
        $scope.hasRoleOfResearcher = false;
        $scope.hasRoleOfOperator = false;
        $scope.hasRoleOfController = false;

        $scope.userProfile = $sessionStorage.userProfile;// authenticationService.getUserProfile();

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

        $scope.mainViewBackdrop = 'enabled-main-view-backdrop';

        // Used to inform user that his token is no longer valid and will be
        // logged out
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

        // Used to inform user (displayed above existing map dialog)
        $scope.showAlertOnMap = function (title, msg) {
            $mdDialog.show(
                    $mdDialog.alert()
                    .parent(angular.element(document.querySelector('#popupContainerOnMap')))
                    .clickOutsideToClose(true)
                    .title(title)
                    .textContent(msg)
                    .ariaLabel('Error Message')
                    .ok('OK')
                    .multiple(true)
                    )
        };

        // Used to know that the treeMenu was opened and because the user opened
        // the Info it was closed
        $scope.treeMenuWasOpen = false;
        $scope.resultItemInfoSidenavWasOpen = false;

        // Toggles SidePanel
        $scope.toggleInfo = infoBuildToggler('rightInfo');
        function infoBuildToggler(componentId) {
            return function () {
                $mdSidenav(componentId).toggle();
                $scope.infoNavIsOpen = $mdSidenav(componentId).isOpen();

                // Regarding Tree Menu SideNav (how to handle when opening the
                // info SideNav at the same time)
                if ($scope.treeMenuIsOpen) {
                    $scope.toggleTreeMenu();
                    $scope.treeMenuWasOpen = true;
                } else if ($scope.treeMenuWasOpen) {
                    $scope.toggleTreeMenu()
                    $scope.treeMenuWasOpen = false;
                }

                // Regarding result item info SideNav (how to handle when
                // opening the info SideNav at the same time)
                if ($mdSidenav('resultItemInfoSidenav').isOpen()) {
                    $mdSidenav('resultItemInfoSidenav').close();
                    $scope.resultItemInfoSidenavWasOpen = true;
                } else if ($scope.resultItemInfoSidenavWasOpen) {
                    $mdSidenav('resultItemInfoSidenav').open();
                    $scope.resultItemInfoSidenavWasOpen = false;
                }

            };
        }

        // Toggles SidePanel for map's dialog panel
        $scope.mapInfoIsOpen = false; // Used for showing the backdrop
        $scope.toggleMapInfo = infoMapToggler('mapInfo');
        function infoMapToggler(componentId) {
            return function () {
                $mdSidenav(componentId).toggle();
                $scope.mapInfoIsOpen = $mdSidenav(componentId).isOpen()
            };
        }

        // Toggles SidePanel for tree menu 
        $scope.toggleTreeMenu = treeMenuBuildToggler('treeMenu');
        function treeMenuBuildToggler(componentId) {
            return function () {
                $mdSidenav(componentId).toggle();
                $scope.treeMenuIsOpen = $mdSidenav(componentId).isOpen();
            };
        }

        $scope.queryFrom = '';

        function constructQueryFrom(namegraphs) {
            $scope.queryFrom = '';
            angular.forEach(namegraphs, function (parentValue, parentKey) {
                angular.forEach(parentValue.children, function (childValue, childKey) {
                    if (childValue.selected) {
                        $scope.queryFrom = $scope.queryFrom + 'from <' + childValue.value + '> ';
                    }
                })
            });
            // In case of none selected, use some fictional namegraph,
            // such that searching in the whole namespace is avoided
            if ($scope.queryFrom == '') {
                $scope.queryFrom = $scope.queryFrom + 'from <http://none> ';
            }
        }

        function makeAllNamegraphsSelected(namegraphs) {
            angular.forEach(namegraphs, function (parentValue, parentKey) {
                parentValue.selected = true;
                angular.forEach(parentValue.children, function (childValue, childKey) {
                    childValue.selected = true;
                })
            });
            return namegraphs;
        }

        function setSelectedtNamegraphs(namegraphs) {
            var foundOne = false;
            angular.forEach(namegraphs, function (parentValue, parentKey) {
                var childNum = 0;
                var tmp = 0;
                angular.forEach(parentValue.children, function (childValue, childKey) {
                    childNum++;
                    if (childValue.selected == true) {
                        foundOne = true;
                        tmp++;
                    }
                    childValue.selected = childValue.selected;
                })
                if (tmp === childNum) {
                    parentValue.selected = true;
                }
            });
            if (!foundOne) {
                makeAllNamegraphsSelected(namegraphs);
            }
            return namegraphs;
        }

        function stringifyReplacerForMenuTree(key, value) {
            if (key == "$$hashKey")
                return undefined;
            else if (key == "__ivhTreeviewExpanded")
                return undefined;
            else if (key == "__ivhTreeviewIndeterminate")
                return undefined;
            else
                return value;
        }

        // Initializing All available entities
        function initAllNamegraphs(newCase) {

            queryService.getDefaultNamegraphs().then(function (response) {
                if (response.status == '200') {
                    $scope.namegraphs = setSelectedtNamegraphs(response.data);
                    // Initializing the queryFrom string
                    constructQueryFrom(response.data);
                    // change : tree is closed at beginning
// if (newCase) { // Not from favorites
// // Initial value for the flag
// $scope.treeMenuIsOpen = true;
// // Open treeMenu
// $scope.toggleTreeMenu();
// }
                    // Keep first copy

                    $scope.namegraphsCopy = angular.copy($scope.namegraphs);
                    initAllEntities($scope.queryFrom, false);
                    homeStateConfirmService.setQueryUnderConstruction(true);
                    $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
                    // favorite
                    // as
                    // changed
                    // (if
                    // favorite)

                    // Initializing the available entities
                    // // NEW added the following in comment!
                    // initAllEntities($scope.queryFrom, false);
                } else if (response.status == '400') {
                    $log.info(response.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                } else if (response.status == '401') {
                    $log.info(response.status);
                    modalInstance.close();
                    $scope.showLogoutAlert();
                    authenticationService.clearCredentials();
                } else {
                    $log.info(response.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                }
            }, function (error) {
                $scope.message = 'There was a network error. Try again later.';
                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                    data: error
                }));
            }).finally(function () {

            });
        }

        /*
         * $scope.namegraphs = [{ id: 'hats', label: 'Hats', children: [ {label:
         * 'Flat cap'}, {label: 'Top hat'}, {label: 'Gatsby'} ] },{ id: 'pens',
         * label: 'Pens', selected: true, children: [ {label: 'Fountain'},
         * {label: 'Gel ink'}, {label: 'Fedora'}, {label: 'Baseball', selected:
         * true}, {label: 'Roller ball'}, {label: 'Fiber tip'}, {label:
         * 'Ballpoint'} ] },{ id: 'whiskey', label: 'Whiskey', children: [
         * {label: 'Irish'}, {label: 'Scotch'}, {label: 'Rye'}, {label:
         * 'Tennessee'}, {label: 'Bourbon'} ] }];
         */

        $scope.awesomeCallback = function (node, tree) {
            // Do something with node or tree
            // alert("awesomeCallback");
        };

        $scope.namegraphTreeCallback = function (ev, node, isSelected, tree) {
            // console.log("namegraphTreeCallback: \nlabel: " + node.label +
            // "\nselected: " + isSelected);
            // console.log(angular.toJson($scope.namegraphs));
            // console.log("namegraphTreeCallback: \nlabel: " + node.label +
            // "\nselected: " + node.selected);

            constructQueryFrom($scope.namegraphs);
            $log.info('$scope.queryFrom: ' + $scope.queryFrom);

        }

        $scope.selectHats = function () {
            // Selecting by node id
            ivhTreeviewMgr.select($scope.namegraphs, 'hats');
        };

        $scope.deselectGel = function () {
            // deselect by node reference
            ivhTreeviewMgr.deselect($scope.namegraphs, $scope.namegraphs[1].children[1]);
        };

        $scope.testBreadcrumb = function (str) {
            alert(str);
        };

        $scope.copyNamegraphs = function () {
            $scope.namegraphsCopy = angular.copy($scope.namegraphs);
        }

        // Clears the whole treeRowModel and loads new entities (on tree-menu
        // hide)
        $scope.reLoadEntities = function (ev, queryFrom) {
            console.log("queryFrom: ");
            console.log(queryFrom);
            var messageContent = 'In order to complete this action, the whole query '
                    + 'constructed so far has to be reset.'
                    + '<br/>'
                    + 'That means that the query has to be re-constructed from scratch. '
                    + '<br/><br/>'
                    + 'Are you sure you want to continue with this action?';

            // showConfirmDialogForUnavailableEntities(ev, messageContent,
            // queryFrom, checkboxNode)
            var confirm = $mdDialog.confirm()
                    .title('Important Message - Confirmation Required')
                    .htmlContent(messageContent)
                    .ariaLabel('Reseting Query - Confirmation')
                    .targetEvent(ev)
                    .ok('Yes Continue')
                    .cancel('Cancel');

            // If target is selected and there are changes on the tree then ask
            // for confirmation
            if (JSON.stringify($scope.namegraphs, stringifyReplacerForMenuTree) != JSON.stringify($scope.namegraphsCopy, stringifyReplacerForMenuTree)) {

                if ($scope.targetModel.selectedTargetEntity != null) {
                    $mdDialog.show(confirm).then(function () {

                        resetWholeQueryModel(true);
                        homeStateConfirmService.setQueryUnderConstruction(true);
                        $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
                        // favorite
                        // as
                        // changed
                        // (if
                        // favorite)
                        $scope.namegraphsCopy = angular.copy($scope.namegraphs);
                        $scope.finalResults = {};

                    }, function () { // Cancel
                        $scope.namegraphs = angular.copy($scope.namegraphsCopy);
                    });
                } else {

                    initAllEntities($scope.queryFrom, false);
                    homeStateConfirmService.setQueryUnderConstruction(true);
                    $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
                    // favorite
                    // as
                    // changed
                    // (if
                    // favorite)
                    $scope.namegraphsCopy = angular.copy($scope.namegraphs);
                    $scope.toggleTreeMenu();
                }

            } else { // No changes
                $scope.toggleTreeMenu();
            }

        }

        $scope.breadcrumbItems =
                [{
                        id: '1',
                        label: 'Frank Norson',
                        icon: 'fa fa-user'
                    }, {
                        id: '2',
                        label: 'Semantic Web Paper',
                        icon: 'fa fa-file-text'
                    }, {
                        id: '3',
                        label: 'Institute of Everything',
                        icon: 'fa fa-building'
                    }, {
                        id: '4',
                        label: 'Happy Life Project',
                        icon: 'fa fa-users'
                    }, {
                        id: '5',
                        label: 'Bloody Jurnal',
                        icon: 'fa fa-file-text'
                    }, {
                        id: '6',
                        label: 'University of Students',
                        icon: 'fa fa-university'
                    }, {
                        id: '7',
                        label: 'East Laos',
                        icon: 'fa fa-globe'
                    }];


        $scope.configuration = {
            wholeTreeModel: {
                levelLimit: 2,
                bothAndOr: false
            },
            everyRowModel: {
                degreeLimit: 2
            },
            targetEntity: {
                excludedEntities: [{name: 'Location'}]
            },
            relatedEntity: {
                map: {
                    maxResoultCountForShowingPinsOnInit: 200,
                    maxNumOfPinsInBoundingBoxOnInit: 400,
                    showPinsWhenDrawingBoundingBox: true,
                    minResoultCountForAutoSelectingPinsOnDrawingBox: 10,
                    alwaysShowPinsForSelectedInstances: true
                },
                excludedEntities: [],
                selectedInstancesLimit: 20
            }
        }

        // Action applied when changing the "showPinsWhenDrawingBoundingBox"
        // configuration option
        $scope.showPinsWhenDrawingBoundingBoxAction = function () {
            // Deactivates the "alwaysShowPinsForSelectedInstances"
            // configuration option
            // when deactivated
            if (!$scope.configuration.relatedEntity.map.showPinsWhenDrawingBoundingBox)
                $scope.configuration.relatedEntity.map.alwaysShowPinsForSelectedInstances = false;
        }

        // To be used for constructing the simple related entity query
        // dynamically
        $scope.relatedEntityQuerySearchText = '';

        // All entities used in the row model
        $scope.allEntities = [];

        // Target model
        $scope.targetModel = {
            selectedTargetEntity: null,
            backupSelectedTargetEntity: null,
            targetEntities: $scope.allEntities, // angular.copy($scope.allEntities);
            searchTargetKeywords: '',
            selectedTargetRecomentation: null,
            targetChips: [],
            backupTargetChips: [],
            availableFilterExpressions: [{expression: 'OR'}, {expression: 'AND'}],
        }

        // Used to convert from UTC (when date is initially null) to local
        // timezone
        var regexMatchForIso8601Date = /[0-9]T[0-9]/; // ISO 8601
        var dateFormat = 'YYYY-MM-DD'; // 'YYYY-MM-DDTHH:mm:ss.sssZ'

        function toLocalTimeZone(value) {
            // $scope.toLocalTimeZone = function (value) {
            if (value != null) {
                // Check that the date is not in ISO 8601 format, if not - then
                // add postfix so can be converted to UTC
                if (!regexMatchForIso8601Date.test(value))
                    value = value + ' UTC';
                if (!moment(value).isValid())
                    return invalidDateMessage;
                return moment(value).format(dateFormat).toLocaleString();
            } else
                null;
        }
        ;

        var autoincrementedRowModelId = 0;

        // Initial empty row model
        $scope.initEmptyRowModel = {
            id: autoincrementedRowModelId,
            level: 0,
            outerSelectedFilterExpression: '',
            availableFilterExpressions: [{expression: 'OR'}, {expression: 'AND'}],
            selectedRelation: null,
            backupSelectedRelation: null,
            relations: [], // angular.copy($scope.relations),
            rangeOfDates: {
                from: null, // new Date(),
                fromInputName: '',
                until: null,
                untilInputName: ''
            },

            backupRangeOfDates: {
                from: null, // new Date(),
                fromInputName: '',
                until: null,
                untilInputName: ''
            },
            numericRange: {
                from: null,
                to: null,
            },

            backupNumericRange: {
                from: null,
                to: null,
            },

            timePrimitive: {
                from: null,
                to: null,
            },

            backupTimePrimitive: {
                from: null,
                to: null,
            },

            booleanValues: {
                yes: false,
                no: false,
            },

            backupBooleanValues: {
                yes: false,
                no: false,
            },

            // boundingBox: null,
            // backupBoundingBox: null,
            selectedRelatedEntity: null, // {name: '', thesaurus: '',
            // queryModel: ''},
            backupSelectedRelatedEntity: null,
            relatedEntities: $scope.allEntities, // angular.copy($scope.allEntities),
            searchRelatedKeywords: '',
            allRelatedSearchResultsIsSelected: true,
            allRelatedEntitiesSelectedList: [{name: 'Search By Keyword'}],
            selectedRelatedInstanceList: [],
            backupSelectedRelatedInstanceList: [],
            shownEntitySearchResults: false,
            selectedRecomentation: null,
            relatedChips: [],
            backupRelatedChips: [],
            relatedEntitySearchText: '',
            rowModelList: [],
            activeRelatedSearchResultsStyle: 'disabled-style', // 'enabled-style'
            activeRowModelStyle: 'disabled-style',
            activeRelationModelStyle: 'disabled-style' // The style was decided
                    // to be distinguished
                    // between relation and
                    // all the rest (related
                    // entity, selected
                    // instances, selected
                    // region)
        }

        $scope.thesaurus = {};

        // Uncheck All-Related-Search-Results option (when removing the single
        // chip)
        $scope.unselectAllRelatedSearchResults = function (rowModel) {
            rowModel.allRelatedSearchResultsIsSelected = false;
            $scope.handleSelectAllRelatedSearchResults(rowModel);
        }

        $scope.currentFavorite = {
            itIsFavorite: false,
            dbTableId: null,
            changed: false
        };

        // Called very often to mark favorite as changed
        $scope.markFavoriteAsChanged = function (changed) {
            if ($scope.currentFavorite != undefined) {
                if ($scope.currentFavorite.itIsFavorite)
                    $scope.currentFavorite.changed = changed;
            }
        }

        $scope.finalQuery = "";

        // Calling service to retrieve service model
        function retrieveserviceModel() {
            queryService.retrieveserviceModel($scope.credentials.token)
                    .then(function (response) {
                        if (response.status == '200') {
                            $scope.serviceModel = response.data;
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
        }

        this.$onInit = function () {

            retrieveserviceModel();

            // Initializing empty row model (new instance)
            $scope.emptyRowModel = angular.copy($scope.initEmptyRowModel);
            $scope.rowModelList = [$scope.emptyRowModel];

            // Initializing target and rowModelList
            // according to whether it is a "new" case or a "loading from
            // favorites" case

            // Initializing model from favorite (if loaded)
            if ($sessionStorage.selectedFavoriteModel != null) {

                // Loading configuration
                if ($sessionStorage.selectedFavoriteModel.queryModel.configuration != null)
                    $scope.configuration = $sessionStorage.selectedFavoriteModel.queryModel.configuration;

                $scope.favoriteTitle = $sessionStorage.selectedFavoriteModel.title;
                $scope.rowModelList = $sessionStorage.selectedFavoriteModel.queryModel.relatedModels;
                $scope.targetModel = $sessionStorage.selectedFavoriteModel.queryModel.targetModel;
                $scope.namegraphs = $sessionStorage.selectedFavoriteModel.queryModel.namegraphs;
                // Keep first copy of namegraphs
                $scope.namegraphsCopy = angular.copy($scope.namegraphs);
                // Construct the "from" section in the query
                constructQueryFrom($scope.namegraphsCopy);

                // Re-retrieve all entities
                initAllEntities($scope.queryFrom, false);

                // Let system know that this is already favorite
                $scope.currentFavorite.itIsFavorite = true;
                $scope.currentFavorite.dbTableId = $sessionStorage.selectedFavoriteModel.favoriteId;
                // Making $sessionStorage.selectedQueryModel null, to free up
                // memory
                $sessionStorage.selectedFavoriteModel = null;
            }
            // New Case
            else {
                initAllNamegraphs(true); // true stands for "new case"
            }

            // Initial value for the Final query (used in code-mirror)
            // initFinalQuery();

        }
        /*
         * function initFinalQuery() { $scope.finalQuery = "Not Yet Defined"; }
         */
        // Initializing All available entities
        function initAllEntities(queryFrom, notify) {

            // initializing is quick, so we don't need the modal. It almost not
            // appear at all!
// var modalOptions = {
// headerText: 'Loading Please Wait...',
// bodyText: 'Initializing available options...'
// };
//
// var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            queryService.getEntities(queryFrom, $scope.credentials.token).then(function (response) {
                if (response.status == '200') {
                    if (response.data.remote_status == 200) {
                        $scope.allEntities = response.data.entities;
                        $scope.targetModel.targetEntities = angular.copy($scope.allEntities); // Such
                        // that
                        // $scope.allEntities
                        // holds
                        // both
                        // excluded
                        // and
                        // non-excluded
                        // ones
                        console.log("All Entities: ");
                        console.log($scope.allEntities);
                        console.log("Target Entities: ");
                        console.log($scope.targetModel.targetEntities);

                        // Remove entities marked as excluded from target entity
                        // list
                        for (var i = $scope.configuration.targetEntity.excludedEntities.length - 1; i >= 0; i--) {
                            var containedInListObject = containedInListBasedOnFieldPath($scope.configuration.targetEntity.excludedEntities[i], $scope.targetModel.targetEntities, 'name');
                            if (containedInListObject.contained) {
                                $scope.targetModel.targetEntities.splice(containedInListObject.index, 1);

                            }
                        }
                        ;

                        // $scope.initEmptyRowModel.relatedEntities =
                        // $scope.allEntities; // No need to load all entities
                        // here

                        if (notify) {
                            // Display msg
                            $mdToast.show(
                                    $mdToast.simple()
                                    .textContent('Query has been reset.')
                                    .position('top right')
                                    .parent(angular.element('#mainContent'))
                                    .hideDelay(3000)
                                    );
                        }
                    } else if (response.data.remote_status == 401) {
                        $log.info(response.data.remote_status);
                        $scope.showLogoutAlert();
                        authenticationService.clearCredentials();
                    } else {
                        $log.info(response.data.remote_status);
                        $scope.message = 'There was an error with the remote server. Try again later and if the same error occures again please contact the administrator.';
                        $scope.showErrorAlert('Error', $scope.message);
                    }
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
            }, function (error) {
                $scope.message = 'There was a network error. Try again later.';
                $scope.showErrorAlert('Error', $scope.message);
            }).finally(function () {
                // modalInstance.close();
            });

        }

        // When clicking on "Using always both regular expressions ('AND' &
        // 'OR')" switcher
        // through the configuration options
        $scope.changeAvailabilityOfRegExpressions = function (bothAndOr) {
            if (bothAndOr) {

                // Changing reg expr options for the target entity
                $scope.targetModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];

                // Changing options for all the related entities
                for (var i = 0; i < $scope.rowModelList.length; i++) {
                    changeAvailabilityOfRegExpressionsRecursively(bothAndOr, $scope.rowModelList[i]);
                }
            } else {
                if ($scope.targetModel.selectedTargetEntity != null) {
                    var message = 'Restricting the available regular expressions while the construction '
                            + 'of the query is under progress is not allowed.'
                            + '<br/><br/>'
                            + 'This action will be rolled back. You can either continue with using all the available regular expressions '
                            + 'or start from scratch by resetting the existing cunstruction.';
                    $mdDialog.show(
                            $mdDialog.alert()
                            .parent(angular.element(document.querySelector('#popupContainerOnConfiguration')))
                            .title('Forbiddance')
                            .htmlContent(message)
                            .ariaLabel('Forbiddance Message')
                            .ok('OK')
                            .multiple(true)
                            ).finally(function () {
                        // Rolling back setting
                        $scope.configuration.wholeTreeModel.bothAndOr = true;
                    });
                }
            }
        }

        // Recursively changing the Changing the reg expr options for each node
        // of the tree
        function changeAvailabilityOfRegExpressionsRecursively(bothAndOr, rowModel) {
            if (bothAndOr) {
                rowModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];
                for (var i = 0; i < rowModel.rowModelList.length; i++) {
                    changeAvailabilityOfRegExpressionsRecursively(bothAndOr, rowModel.rowModelList[i]);
                }
            }
        }

        $scope.addNewEmptyRowModel = function (parentRowModel, str) {

            autoincrementedRowModelId++;

            if (parentRowModel == null) {

                if ($scope.configuration.wholeTreeModel.bothAndOr == false)
                    $scope.targetModel.availableFilterExpressions = [{expression: str}];
                // else
                // $scope.targetModel.availableFilterExpressions = [{expression:
                // 'OR'}, {expression: 'AND'}];

                $scope.rowModelList.push(angular.copy($scope.initEmptyRowModel));
                $scope.rowModelList[$scope.rowModelList.length - 1].outerSelectedFilterExpression = str;
                $scope.rowModelList[$scope.rowModelList.length - 1].id = autoincrementedRowModelId;

                // Loading related entities and relations for new related model
                $scope.loadRelatedEntitiesAndRelationsByTarget('no-event', 'Not-Needed', undefined, $scope.targetModel.selectedTargetEntity, 'addFilter');

                // Enabling rowModel
                $scope.rowModelList[$scope.rowModelList.length - 1].activeRowModelStyle = 'enabled-style';
            } else { // if(parentRowModel != null) {

                if ($scope.configuration.wholeTreeModel.bothAndOr == false)
                    parentRowModel.availableFilterExpressions = [{expression: str}];
                // else
                // parentRowModel.availableFilterExpressions = [{expression:
                // 'OR'}, {expression: 'AND'}];

                parentRowModel.rowModelList.push(angular.copy($scope.initEmptyRowModel));
                parentRowModel.rowModelList[parentRowModel.rowModelList.length - 1].outerSelectedFilterExpression = str;
                parentRowModel.rowModelList[parentRowModel.rowModelList.length - 1].id = autoincrementedRowModelId;

                // Loading related entities and relations for new related model
                $scope.loadRelatedEntitiesAndRelationsByTarget('no-event', 'Not-Needed', parentRowModel, parentRowModel.selectedRelatedEntity, 'addFilter');

                // Enabling rowModel
                parentRowModel.rowModelList[parentRowModel.rowModelList.length - 1].activeRowModelStyle = 'enabled-style';

                // Increment level by one considering the parent's level
                parentRowModel.rowModelList[parentRowModel.rowModelList.length - 1].level = parentRowModel.level + 1;
            }

        }

        $scope.deactivaeRowModel = function (outerIndex, rowModel) {
            $scope.removeRowModel(outerIndex, rowModel)
            $scope.rowModelList.splice(outerIndex + 1, 1);
            $scope.rowModelList[$scope.rowModelList.length - 1].activeRowModelStyle = 'disabled-style';
            rowModel.selectedRelatedEntity = null;
            rowModel.selectedRelation = null;
            rowModel.activeRelationModelStyle = 'disabled-style';
            ;
            homeStateConfirmService.setQueryUnderConstruction(true);
            $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
            // favorite
            // as
            // changed
            // (if
            // favorite)
        }


        $scope.removeRowModel = function (outerIndex, rowModel) {
            // Removes 1 item at position outerIndex
            if (rowModel == null) {
                $scope.rowModelList.splice(outerIndex, 1);
                if ($scope.rowModelList.length < 2) // Restore logical options
                    // if only one left
                    $scope.targetModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];
            } else { // if(rowModel != null) {
                rowModel.rowModelList.splice(outerIndex, 1);
                if (rowModel.rowModelList.length < 2) // Restore logical
                    // options if only one
                    // left
                    rowModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];
            }
            homeStateConfirmService.setQueryUnderConstruction(true);
            $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
            // favorite
            // as
            // changed
            // (if
            // favorite)
        }

        $scope.changeLogicalExpressionOfRowModel = function (rowModel) {
            if (rowModel.outerSelectedFilterExpression == 'OR') {
                rowModel.outerSelectedFilterExpression = 'AND';
            } else { // if(rowModel.outerSelectedFilterExpression=='AND')
                rowModel.outerSelectedFilterExpression = 'OR';
            }
        }

        $scope.enableFirstRelatedEntity = function (ev) {
            // Enabling rowModel
            $scope.rowModelList[$scope.rowModelList.length - 1].activeRowModelStyle = 'enabled-style';
            $scope.loadRelatedEntitiesAndRelationsByTarget(ev, null, null, $scope.targetModel.selectedTargetEntity, 'addVeryFirstFilter');
        }

        // Delete Useless for the back-end properties, occupying a lot of volume
        function deleteUselessForBackendInformation(model) {

            // ----------- model.queryModel:

            // Target
            delete model.queryModel.targetModel.backupSelectedTargetEntity;
            delete model.queryModel.targetModel.backupTargetChips;
            delete model.queryModel.targetModel.targetEntities;

            // Related Entity List (whole (model.queryModel))
            for (var i = 0; i < model.queryModel.relatedModels.length; i++) {
                deleteUselessForBackEndRelatedProperties(model.queryModel.relatedModels[i], true);
            }

            // ----------- model.rowModel"

            // Related Entity List (just the current rowModel))
            if (model.rowModel != undefined) {
                delete model.rowModel.backupSelectedRelatedEntity;
                delete model.rowModel.backupRelatedChips;
                delete model.rowModel.backupSelectedRelation;
                delete model.rowModel.backupSelectedRelatedInstanceList;
                delete model.rowModel.backupRangeOfDates;
                delete model.rowModel.backupNumericRange;
                delete model.rowModel.backupTimePrimitive;
                delete model.rowModel.backupBooleanValues;

                delete model.rowModel.relatedEntities;
                delete model.rowModel.relations;

                // Delete for children recursively
                deleteUselessForBackEndRelatedProperties(model.rowModel, false);
            }

            // console.log(angular.toJson("###############################################################"));
            // console.log(angular.toJson(model));
            // console.log(angular.toJson("###############################################################"));
            return model;
        }

        $scope.loadRelatedEntitiesByRelation = function (parentRowModel, rowModel) {

            // Param-model for the new service
            var model = {
                queryFrom: $scope.queryFrom,
                rowModel: angular.copy(rowModel),
                queryModel: {
                    targetModel: angular.copy($scope.targetModel),
                    relatedModels: angular.copy($scope.rowModelList)
                }
            }

            // Delete Useless for the back-end properties, occupying a lot of
            // volume
            model = deleteUselessForBackendInformation(model);

            if (rowModel.selectedRelation != null && rowModel.selectedRelation != undefined) {
                if (rowModel.selectedRelation.relatedEntity != null && rowModel.selectedRelation.relatedEntity != undefined) {
                    // console.log('selectedRelation.relatedEntity' +
                    // angular.toJson(rowModel.selectedRelation.relatedEntity));
                    angular.forEach(rowModel.relatedEntities, function (relatedEntity, key) {
                        if (relatedEntity.uri == rowModel.selectedRelation.relatedEntity.uri) {

                            // Setting in the UI the selected related entity
                            rowModel.selectedRelatedEntity = relatedEntity; // Not
                            // needed
                            // for
                            // the
                            // service
                            // that
                            // uses
                            // model

                            // Setting in the model parameter the selected
                            // relatedEntity
                            model.rowModel.selectedRelatedEntity = relatedEntity;
                            if (parentRowModel == undefined) {
                                paramModelForRelations = {
                                    model: model
                                }
                            }

                            // Case - Target is related entity
                            else {
                                paramModelForRelations = {
                                    model: model
                                }
                            }

                            // Updating the list of relations based on the user
                            // selected target and the auto-completed related
                            // entities
                            handleRelationsByTargetAndRelatedEntities(rowModel, paramModelForRelations, $scope.credentials.token);

                        }
                    });
                }
            }

            homeStateConfirmService.setQueryUnderConstruction(true);
            $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
            // favorite
            // as
            // changed
            // (if
            // favorite)

        }

        // Loading the list of relations and related entities
        // based on the selected entity
        // param: parentRowModel the parentRowModel (can be null or undefined)
        // param: rowModel the currentRowModel (can be undefined)
        // param: selectedEntity the selected Entity (acting as target)
        // param: provenanceFunction A string denoting provenance ('addFilter',
        // 'levelDown', 'relatedEntitySelect',
        // 'targetEntitySelect', 'targetChipChange')
        // (In case of selecting real target entity, the parent rowModel is
        // null,
        // the rowModel is also undefined and the selecteEntity is the
        // selectedTargetEntity)
        $scope.loadRelatedEntitiesAndRelationsByTarget = function (ev, parentRowModel, rowModel, selectedEntity, provenanceFunction) {
            console.log(">>> selectedEntity: ");
            console.log(selectedEntity);

            if (selectedEntity != null) {
                console.log("the selected entity is NOT null!");
                // Param-model for the new service
                var model = {
                    queryFrom: $scope.queryFrom,
                    rowModel: angular.copy(rowModel),
                    // logicalExpression: str,
                    queryModel: {
                        targetModel: angular.copy($scope.targetModel),
                        relatedModels: angular.copy($scope.rowModelList)
                    }
                }

                // Delete Useless for the back-end properties, occupying a lot
                // of volume
                model = deleteUselessForBackendInformation(model);

                // Rrelated Entity List handling

                // Parameters to sent for the Relations And Related Entities
                // Service (the same for all cases)
                var paramModelForRelationsAndRelatedEntities = {
                    entities: $scope.allEntities, // The list of all entities
                    model: model
                }


                console.log("provenance function: " + provenanceFunction);

                // Case where entity selection is from target
                if (rowModel == undefined) {
                    if (provenanceFunction == 'addVeryFirstFilter') {
                        // Will actually loop only once
                        console.log($scope.rowModelList.length);
                        for (var i = 0; i < $scope.rowModelList.length; i++) {
                            handleRelationsAndRelatedEntitiesByTarget(null, $scope.rowModelList[i],
                                    paramModelForRelationsAndRelatedEntities, $scope.credentials.token);
                        }
                        // Holding Copy Of Selected Target Entity
                        $scope.targetModel.backupSelectedTargetEntity = angular.copy($scope.targetModel.selectedTargetEntity);
                        // Holding Copy Of Selected Target Entity
                        $scope.targetModel.backupTargetChips = angular.copy($scope.targetModel.targetChips);
                    }

                    // When adding new filter
                    else if (provenanceFunction == 'addFilter') {

                        // Add logical expression to model
                        paramModelForRelationsAndRelatedEntities.logicalExpression =
                                model.queryModel.relatedModels[model.queryModel.relatedModels.length - 1].outerSelectedFilterExpression;

                        if ($scope.rowModelList.length > 0) {
                            handleRelationsAndRelatedEntitiesByTarget(null, $scope.rowModelList[$scope.rowModelList.length - 1],
                                    paramModelForRelationsAndRelatedEntities, $scope.credentials.token);

                        }
                    }

                    // When selecting target entity or adding/removing a chiip
                    else if (provenanceFunction == 'targetEntitySelect' || provenanceFunction == 'targetChipChange') {
                        var isEditAction = false;
                        // For each related entity - level 1
                        for (var i = 0; i < $scope.rowModelList.length; i++) {
                            if ($scope.rowModelList[i].selectedRelatedEntity != null || $scope.rowModelList[i].selectedRelation != null)
                                isEditAction = true;
                            break;
                        }

                        // If this is not the initial selection (user is
                        // editing)
                        if (isEditAction) {
                            console.log("isEditAction");
                            var messageContent = 'In order to complete this action, the query in terms of related options has to be reset.'
                                    + '<br/>'
                                    + 'That means that this part of the query has to be re-constructed from scratch.'
                                    + '<br/><br/>'
                                    + 'Are you sure you want to continue with this action?';

                            var confirm = $mdDialog.confirm()
                                    .title('Important Message - Confirmation Required')
                                    .htmlContent(messageContent)
                                    .ariaLabel('Reseting Query - Confirmation')
                                    .targetEvent(ev)
                                    .ok('Yes Continue')
                                    .cancel('Cancel');

                            $mdDialog.show(confirm).then(function () { // OK
                                resetRelatedModel(); // Resetting the whole
                                // related model
                            }, function () { // Cancel  
                                // Load previous selection (the backup)
                                //for (var i = 0; i < $scope.targetModel.targetEntities.length; i++) {
                                //if ($scope.targetModel.targetEntities[i].uri == $scope.targetModel.backupSelectedTargetEntity.uri)
                                //  $scope.targetModel.selectedTargetEntity = $scope.targetModel.targetEntities[i];
                                //  }

                                // Load previous chips from backup
                                $scope.targetModel.targetChips = angular.copy($scope.targetModel.backupTargetChips);

                            });
                            /*
                             * .finally(function() {
                             * console.log($scope.rowModelList[0].selectedRelatedEntity.name);
                             * });
                             */
                        } else { // if(!isEditAction) {
                            resetRelatedModel(); // Resetting the whole
                            // related model
                        }

                        // Initializing the whole related model
                        function resetRelatedModel() {

                            // Create an empty model
                            $scope.emptyRowModel = angular.copy($scope.initEmptyRowModel);
                            // Enable the style for it
                            $scope.emptyRowModel.activeRowModelStyle = 'disabled-style';
                            // Delete all the children and add only one (the
                            // empty one just created)
                            $scope.rowModelList = [$scope.emptyRowModel];
                            // Initialize available logical expressions
                            $scope.targetModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];

                            var rowModelId = $scope.rowModelList[0].id;
                            // $scope.searchForm['relatedEntityInput_' +
                            // $scope.emptyRowModel.id].$setTouched();
                            $scope.searchForm['relatedEntityInput_' + $scope.emptyRowModel.id].$setUntouched();
                            $scope.searchForm['relationInput_' + $scope.emptyRowModel.id].$setUntouched();


                            // Holding Copy Of Selected Target Entity
                            if (provenanceFunction == 'targetEntitySelect')
                                $scope.targetModel.backupSelectedTargetEntity = angular.copy($scope.targetModel.selectedTargetEntity);

                            // Holding Copy Of Selected Target Entity
                            if (provenanceFunction == 'targetChipChange')
                                $scope.targetModel.backupTargetChips = angular.copy($scope.targetModel.targetChips);
                        }

                    } // if(provenanceFunction == 'targetEntitySelect') - Ends

                    else { // provenanceFunction is: levelDown
                        for (var i = 0; i < $scope.rowModelList.length; i++) {
                            handleRelationsAndRelatedEntitiesByTarget(null, $scope.rowModelList[i],
                                    paramModelForRelationsAndRelatedEntities, $scope.credentials.token);
                        }
                    }
                    // Enabling rowModel
                    // $scope.rowModelList[$scope.rowModelList.length-1].activeRowModelStyle
                    // = 'enabled-style';
                }

                // Case where entity selection is from the related entity
                else {
                    var isEditAction = false; // Used to catch edit action

                    // Relation parameters - Start

                    // Parameters to sent for the Relations Service (considering
                    // both target and related entity)
                    var paramModelForRelations = {}; // Initializing

                    // There is no parent
                    if (parentRowModel == undefined) {
                        paramModelForRelations = {
                            model: model
                        }
                    }
                    // Case - Level Down
                    else if (provenanceFunction == 'levelDown') {
                        // Do nothing
                        console.log('Level Downn');
                    } else if (provenanceFunction == 'addFilter') {
                        // Do nothing
                        console.log('addFilter');
                    }
                    // Case - Target is related entity
                    else {
                        paramModelForRelations = {
                            model: model
                        }
                    }

                    // Relation Parameters - Ends

                    // When adding new filter
                    if (provenanceFunction == 'addFilter') {

                        // Add logical expression to model
                        paramModelForRelationsAndRelatedEntities.logicalExpression =
                                model.rowModel.rowModelList[model.rowModel.rowModelList.length - 1].outerSelectedFilterExpression;

                        if ($scope.rowModelList.length > 0) {
                            handleRelationsAndRelatedEntitiesByTarget(rowModel, rowModel.rowModelList[rowModel.rowModelList.length - 1],
                                    paramModelForRelationsAndRelatedEntities, $scope.credentials.token);
                        }
                    }

                    // When selecting related entity
                    else if (provenanceFunction == 'relatedEntitySelect' ||
                            provenanceFunction == 'relatedChipChange' ||
                            provenanceFunction == 'relatedListOfInstancesChange' ||
                            provenanceFunction == 'relatedFromDateChange' ||
                            provenanceFunction == 'relatedUntilDateChange' ||
                            provenanceFunction == 'relatedFromNumericChange' ||
                            provenanceFunction == 'relatedToNumericChange') {

                        // Fix dates to local timezone
                        if (provenanceFunction == 'relatedFromDateChange')
                            rowModel.rangeOfDates.from = toLocalTimeZone(rowModel.rangeOfDates.from);
                        if (provenanceFunction == 'relatedUntilDateChange')
                            rowModel.rangeOfDates.until = toLocalTimeZone(rowModel.rangeOfDates.until);


                        // Set edit action if there are any children defined
                        if (rowModel.rowModelList.length > 0)
                            isEditAction = true;

                        // Set edit action specifically when changing the
                        // selected
                        // entity or the chips and there are selected instances
                        // defined
                        if (!isEditAction) {
                            if (provenanceFunction == 'relatedEntitySelect' ||
                                    provenanceFunction == 'relatedChipChange' ||
                                    provenanceFunction == 'relatedFromDateChange' ||
                                    provenanceFunction == 'relatedUntilDateChange' ||
                                    provenanceFunction == 'relatedFromNumericChange' ||
                                    provenanceFunction == 'relatedToNumericChange') {
                                if (rowModel.selectedRelatedInstanceList.length > 0)
                                    isEditAction = true;
                            }
                        }

                        // If this is not the initial selection (user is
                        // editing)
                        if (isEditAction) {

                            var messageContent = 'In order to complete this action, the query after the point of '
                                    + 'your change has to be reset.'
                                    + '<br/>'
                                    + 'That means that this part of the query has to be re-constructed from scratch. '
                                    + '<br/><br/>'
                                    + 'Are you sure you want to continue with this action?';

                            var confirm = $mdDialog.confirm()
                                    .title('Important Message - Confirmation Required')
                                    .htmlContent(messageContent)
                                    .ariaLabel('Reseting Query Partially - Confirmation')
                                    .targetEvent(ev)
                                    .ok('Yes Continue')
                                    .cancel('Cancel');

                            $mdDialog.show(confirm).then(function () { // OK

                                // confirmToContinue = true;

                                // Delete all children of this rowModel
                                rowModel.rowModelList = [];

                                // Reconstructing the model to send
                                paramModelForRelationsAndRelatedEntities.model.rowModel = angular.copy(rowModel);
                                paramModelForRelationsAndRelatedEntities.model.queryModel.relatedModels = angular.copy($scope.rowModelList);

                                // Delete Useless for the back-end properties,
                                // occupying a lot of volume
                                model = deleteUselessForBackendInformation(model);

                                // Specifically when changing the selected
                                // entity
                                if (provenanceFunction == 'relatedEntitySelect') {
                                    // Holding Copy Of Selected Related Entity
                                    rowModel.backupSelectedRelatedEntity = angular.copy(rowModel.selectedRelatedEntity);

                                    // Relation List Handling (Applied after
                                    // handling the relatedEntity)
                                    handleRelationsByTargetAndRelatedEntities(rowModel, paramModelForRelations, $scope.credentials.token);
                                }

                                // Specifically when changing chips or instances

                                // Holding Copy of the Chips for next time
                                if (provenanceFunction == 'relatedChipChange')
                                    rowModel.backupRelatedChips = angular.copy(rowModel.relatedChips);
                                // Keeping backup of the
                                // selectedRelatedInstanceList
                                if (provenanceFunction == 'relatedListOfInstancesChange')
                                    rowModel.backupSelectedRelatedInstanceList = angular.copy(rowModel.selectedRelatedInstanceList);

                                // in certain case the selected instances have
                                // to be cleared
                                // (when changing the selected related entity or
                                // the related selected chips)
                                if (provenanceFunction == 'relatedEntitySelect' ||
                                        provenanceFunction == 'relatedChipChange' ||
                                        provenanceFunction == 'relatedFromDateChange' ||
                                        provenanceFunction == 'relatedUntilDateChange') {
                                    // Clearing list of Instances and hiding
                                    // respective panel
                                    // If the list of instances to be loaded
                                    // (from the backup) is empty, then hide the
                                    // respective panel
                                    if (rowModel.selectedRelatedInstanceList.length > 0) {
                                        rowModel.selectedRelatedInstanceList = [];
                                        $scope.showEntitySearchResults(rowModel, false);
                                    }
                                }

                                // Keeping backup of the rangeOfDatesDefined
                                if (provenanceFunction == 'relatedFromDateChange' ||
                                        provenanceFunction == 'relatedUntilDateChange')
                                    rowModel.backupRangeOfDates = angular.copy(rowModel.rangeOfDates);

                                // Keeping backup of the rangeOfNumericDefined
                                if (provenanceFunction == 'relatedFromNumericChange' ||
                                        provenanceFunction == 'relatedToNumericChange')
                                    rowModel.backupNumericRange = angular.copy(rowModel.numericRange);

                                if (provenanceFunction == 'relatedFromTimePrimitiveChange' ||
                                        provenanceFunction == 'relatedToTimePrimitiveChange')
                                    rowModel.backupTimePrimitive = angular.copy(rowModel.timePrimitive);

                                if (provenanceFunction == 'relatedBooleanYes' ||
                                        provenanceFunction == 'relatedBooleanNo')
                                    rowModel.backupBooleanValues = angular.copy(rowModel.booleanValues);

                                // Display msg
                                $mdToast.show(
                                        $mdToast.simple()
                                        .textContent('Query has been partly reset.')
                                        .position('top right')
                                        .parent(angular.element('#mainContent'))
                                        .hideDelay(3000)
                                        );

                            }, function () { // Cancel
                                confirmToContinue = false;
                                // Load previous selection (the backup) -
                                // Selected Related Entity
                                for (var i = 0; i < rowModel.relatedEntities.length; i++) {
                                    if (rowModel.relatedEntities[i].uri == rowModel.backupSelectedRelatedEntity.uri)
                                        rowModel.selectedRelatedEntity = rowModel.relatedEntities[i];
                                }

                                // Specifically when chips or instances were
                                // changed but canceled

                                // Load previous list of chips (backup)
                                if (provenanceFunction == 'relatedChipChange')
                                    rowModel.relatedChips = angular.copy(rowModel.backupRelatedChips);

                                // Load previous list of selected related
                                // Instances and bounding box
                                if (provenanceFunction == 'relatedListOfInstancesChange') {
                                    rowModel.selectedRelatedInstanceList = angular.copy(rowModel.backupSelectedRelatedInstanceList);

                                    // If the list of instances to be loaded
                                    // (from the backup) is empty, then hide the
                                    // respective panel
                                    if (rowModel.selectedRelatedInstanceList.length < 1)
                                        $scope.showEntitySearchResults(rowModel, false);
                                    else
                                        $scope.showEntitySearchResults(rowModel, true);

                                    // Load previous boundingBox if there was
                                    // any
                                    if (rowModel.backupBoundingBox != null)
                                        rowModel.boundingBox = angular.copy(rowModel.backupBoundingBox);

                                }

                                // Load previous range of dates (backup)
                                if (provenanceFunction == 'relatedFromDateChange' ||
                                        provenanceFunction == 'relatedUntilDateChange')
                                    rowModel.rangeOfDates = angular.copy(rowModel.backupRangeOfDates);

                                // Load previous range of numeric (backup)
                                if (provenanceFunction == 'relatedFromNumericChange' ||
                                        provenanceFunction == 'relatedToNumericChange')
                                    rowModel.numericRange = angular.copy(rowModel.backupNumericRange);

                                if (provenanceFunction == 'relatedFromTimePrimitiveChange' ||
                                        provenanceFunction == 'relatedToTimePrimitiveChange')
                                    rowModel.timePrimitive = angular.copy(rowModel.backupTimePrimitive);

                                if (provenanceFunction == 'relatedFromTimePrimitiveChange' ||
                                        provenanceFunction == 'relatedToTimePrimitiveChange')
                                    rowModel.timePrimitive = angular.copy(rowModel.backupTimePrimitive);

                                if (provenanceFunction == 'relatedBooleanYes' ||
                                        provenanceFunction == 'relatedBooleanNo')
                                    rowModel.backupBooleanValues = angular.copy(rowModel.booleanValues);

                            });
                        } else { // if(!isEditAction)

                            // Holding Copy Of Selected Related Entity
                            if (provenanceFunction == 'relatedEntitySelect') {
                                rowModel.backupSelectedRelatedEntity = angular.copy(rowModel.selectedRelatedEntity);
                                // Relation List Handling (Applied after
                                // handling the relatedEntity)
                                handleRelationsByTargetAndRelatedEntities(rowModel, paramModelForRelations, $scope.credentials.token);
                            }

                            // Holding Copy of the Chips for next time
                            if (provenanceFunction == 'relatedChipChange')
                                rowModel.backupRelatedChips = angular.copy(rowModel.relatedChips);
                            // Keeping backup of the selectedRelatedInstanceList
                            if (provenanceFunction == 'relatedListOfInstancesChange')
                                rowModel.backupSelectedRelatedInstanceList = angular.copy(rowModel.selectedRelatedInstanceList);
                            // / Keeping backup of the rangeOfDatesDefined
                            if (provenanceFunction == 'relatedFromDateChange' ||
                                    provenanceFunction == 'relatedUntilDateChange')
                                rowModel.backupRelatedChips = angular.copy(rowModel.rangeOfDates);

                            // Keeping backup of the rangeOfNumericDefined
                            if (provenanceFunction == 'relatedFromNumericChange' ||
                                    provenanceFunction == 'relatedToNumericChange')
                                rowModel.backupNumericRange = angular.copy(rowModel.numericRange);

                            if (provenanceFunction == 'relatedFromTimePrimitiveChange' ||
                                    provenanceFunction == 'relatedToTimePrimitiveChange')
                                rowModel.backupTimePrimitive = angular.copy(rowModel.timePrimitive);

                            if (provenanceFunction == 'relatedBooleanYes' ||
                                    provenanceFunction == 'relatedBooleanNo')
                                rowModel.backupBooleanValues = angular.copy(rowModel.booleanValues);

                        }
                    } // if(provenanceFunction == 'relatedEntitySelect') -
                    // Ends

                    else { // provenanceFunction is: levelDown
                        for (var i = 0; i < rowModel.rowModelList.length; i++) {
                            handleRelationsAndRelatedEntitiesByTarget(rowModel, rowModel.rowModelList[i],
                                    paramModelForRelationsAndRelatedEntities, $scope.credentials.token);
                        }
                        // Holding Copy Of Selected Related Entity
                        rowModel.backupSelectedRelatedEntity = angular.copy(rowModel.selectedRelatedEntity);
                        // Holding Copy of the Chips for next time
                        rowModel.backupRelatedChips = angular.copy(rowModel.relatedChips);
                    }

                    // Enabling style for the relation drop down list
                    rowModel.activeRelationModelStyle = 'enabled-style';

                } // Close - else (selection from related entity

            } // If close - (selectedEntity not null)

            // Setting flag that makes clear that a query is under construction
            // (used to avoid leaving without confirmation)
            homeStateConfirmService.setQueryUnderConstruction(true);
            $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
            // favorite
            // as
            // changed
            // (if
            // favorite)

        }

        // Handling Relations and Related Entity By Target
        function handleRelationsAndRelatedEntitiesByTarget(parentRowModel, rowModel, paramModel, token) {

            // console.log('handleRelationsAndRelatedEntitiesByTarget(): ');
            // console.log(angular.toJson(paramModel));

            // Modal here is very desturbing
            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Updating available options...'
            };

            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            // Make the model String (more convenient for the back-end)
            paramModel.model = angular.toJson(paramModel.model);

            queryService.getRelationsAndRelatedEntitiesByTarget(paramModel, token)
                    .then(function (response) {

                        if (response.status == -1) {
                            $scope.message = 'There was a network error. Try again later.';
                            $scope.showErrorAlert('Error', $scope.message);
                        } else {
                            if (response.status == '200') {

                                if (response.data.length > 0) {

                                    rowModel.selectedRelatedEntity = null;

                                    rowModel.rowModelList = [];
                                    // $scope.searchForm['relatedEntityInput_' +
                                    // rowModel.id].$setValidity('required', false);
                                    rowModel.relations = [];
                                    rowModel.selectedRelation = null;

                                    // Storing response in the rowModel
                                    rowModel.relatedEntityRelationTuples = angular.toJson(response.data);

                                    for (var i = 0; i < response.data.length; i++) {
                                        // Check for duplicates (URI based) in the
                                        // list of related entities
                                        // Pure compare
                                        if (!containedInList(response.data[i].related_entity, rowModel.relatedEntities, false).contained)
                                            rowModel.relatedEntities.push(response.data[i].related_entity);

                                        rowModel.relations.push(response.data[i].relation);
                                        rowModel.relations[i].relatedEntity = response.data[i].related_entity;
                                        // console.log(response.data[i].relation);
                                        // console.log(response.data[i].related_entity);
                                        // Check if the relation's label is
                                        // duplicated and mark it as duplicated
                                        // if(containedInListManyTimesBasedOnFieldPathOfDepth2(response.data[i].relation,
                                        // response.data, 'name', 'relation',
                                        // 'name').index.length >1)
                                        // rowModel.relations[i].duplicate = true;
                                        // $log.info('value: ' + value);
                                    }

                                    // Sort them by label
                                    rowModel.relations = $filter('orderBy')(rowModel.relations, 'name');

                                    // Display msg
                                    $mdToast.show(
                                            $mdToast.simple()
                                            .textContent('Selection Options for related entities have been updated.')
                                            .position('top right')
                                            .parent(angular.element('#mainContent'))
                                            .hideDelay(3000)
                                            );
                                } else { // if(response.data.length <= 0)

                                    if (parentRowModel != null) {// Parent is
                                        // some
                                        // entityModel
                                        parentRowModel.rowModelList.splice(parentRowModel.rowModelList.length - 1, 1);
                                        if (parentRowModel.rowModelList.length < 2) // Restore
                                            // logical
                                            // options
                                            // if
                                            // only
                                            // one
                                            // left
                                            parentRowModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];
                                    } else { // Parent is target
                                        if ($scope.rowModelList.length == 1) // This
                                            // is
                                            // the
                                            // only
                                            // rowmodel
                                            resetWholeQueryModel(false);
                                        else // /There are more than one row
                                            // models (target has many children)
                                            $scope.rowModelList.splice($scope.rowModelList.length - 1, 1);

                                        if ($scope.rowModelList.length < 2) // Restore
                                            // logical
                                            // options
                                            // if
                                            // only
                                            // one
                                            // left
                                            $scope.targetModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];
                                    }

                                    // I cannot get the parentRowModel, thus
                                    // prompting a general message
                                    $scope.message = 'The selected options lead to no results.';
                                    $scope.showErrorAlert('Information', $scope.message);
                                }

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
                modalInstance.close();
            });
        }

        /*
         * // Recursive method that makes all selected related entities in a
         * tree rowmodel null function
         * makeAllChildrenSelectedRelatedEntitiesNull(rowModel) {
         * rowModel.selectedRelatedEntity = null; // For all children for(var
         * i=0; i<rowModel.rowModelList.length; i++) {
         * rowModel.rowModelList[i].selectedRelatedEntity = null; // Recursively
         * makeAllChildrenSelectedRelatedEntitiesNull(rowModel.rowModelList[i]); } }
         */

        // Handling relations based on target and related entities
        function handleRelationsByTargetAndRelatedEntities(rowModel, paramModel, token) {

            // Make the model String (more convenient for the back-end)
            paramModel.model = angular.toJson(paramModel.model);

            queryService.getRelationsByTargetAndRelatedEntity(paramModel, token)
                    .then(function (response) {

                        if (response.status == -1) {
                            $scope.message = 'There was a network error. Try again later.';
                            $scope.showErrorAlert('Error', $scope.message);
                        } else {
                            if (response.status == '200') {
                                // Response is formed like this:
                                // [{name:"RELATION_NAME",uri:"SOME_URI"}, ... ]

                                // Constructing relation List and related entity
                                // list
                                rowModel.relations = [];
                                for (var i = 0; i < response.data.length; i++) {
                                    rowModel.relations.push(response.data[i]);
                                }

                                // Re-setting selected relation if still available
                                if (rowModel.selectedRelation != null) {
                                    var found = false;
                                    for (var i = 0; i < rowModel.relations.length; i++) {
                                        if (rowModel.relations[i].uri == rowModel.selectedRelation.uri) {
                                            rowModel.selectedRelation = rowModel.relations[i];
                                            found = true;
                                            // break;
                                        }
                                    }
                                    if (found == false) {
                                        rowModel.backupSelectedRelation = angular.copy(rowModel.selectedRelation);
                                        rowModel.selectedRelation = null;
                                    }
                                }
                                if (rowModel.backupSelectedRelation != null)
                                    console.log('rowModel.backupSelectedRelation.name: ' + rowModel.backupSelectedRelation.name)

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
                    });
        }

        // Adding filter on related entity
        $scope.addFilterOnRelated = function (parentRowModel, rowModel) {
            autoincrementedRowModelId++;
            rowModel.rowModelList.push(angular.copy($scope.initEmptyRowModel));
            // rowModel.rowModelList[outerIndex].activeStyle = 'enabledStyle';
            rowModel.rowModelList[rowModel.rowModelList.length - 1].id = autoincrementedRowModelId;
            // Increment level by one considering the parent's level
            rowModel.rowModelList[rowModel.rowModelList.length - 1].level = rowModel.level + 1;
            // Enabling rowModel
            rowModel.rowModelList[rowModel.rowModelList.length - 1].activeRowModelStyle = 'enabled-style';

            // Loading related entities and relations for new related model
            $scope.loadRelatedEntitiesAndRelationsByTarget('no-event', 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'levelDown');

        }

        // Recursive Method used for development only (so far), which deletes
        // all these properties that
        // occupy a big volume in the rowModelList
        // Parameters: the rowModel and a boolean denoting whether to delete the
        // relatedEntityRelationTuples
        function deleteUselessForBackEndRelatedProperties(rowModel, deleteRelatedEntityRelationTuples) {

            // Used for avoiding deleting for the parameter named rowModel (but
            // delete for the relatedModels list)
            if (deleteRelatedEntityRelationTuples)
                delete rowModel.relatedEntityRelationTuples;

            delete rowModel.backupSelectedRelatedEntity;
            delete rowModel.backupRelatedChips;
            delete rowModel.backupSelectedRelation;
            delete rowModel.backupSelectedRelatedInstanceList;
            delete rowModel.backupRangeOfDates;
            delete rowModel.backupNumericRange;
            delete rowModel.backupTimePrimitive;
            delete rowModel.backupBooleanValues;
            delete rowModel.relatedEntities;
            delete rowModel.relations;

            // For all children
            for (var i = 0; i < rowModel.rowModelList.length; i++) {
                // Recursively (always deleting the relatedEntityRelationTuples)
                deleteUselessForBackEndRelatedProperties(rowModel.rowModelList[i], true);
            }
        }

        // SpeedDialModes
        $scope.targetSpeedDialMode = 'md-scale'; // 'md-scale'
        $scope.relatedSpeedDialMode = 'md-scale';


        $scope.selectRelatedEntityFromResults = function (rowModel, index) {
            rowModel.selectedRelatedInstanceList = [{name: rowModel.relatedEntityResults[index].name}];
        };

        $scope.pressEnterOnEntitySearchResults = function (outerIndex, keyEvent) {
            if (keyEvent.which === 13)
                $scope.showEntitySearchResults(outerIndex, true);
        }

        // Adding list of results to the respective instance of the rowModel
        $scope.showEntitySearchResults = function (rowModel, boolean) {
            rowModel.shownEntitySearchResults = boolean;

            // Respectively handle rowModel.allRelatedSearchResultsIsSelected
            rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
            $scope.handleSelectAllRelatedSearchResults(rowModel);
        }

        // some animate example
        // http://plnkr.co/edit/oCLzLHbDLtNT8G8rKFJS?p=preview

        // Thesaurus

        $scope.loadThesaurus = function (entity, outerIndex) {
            if (entity != null) {
                if (entity.thesaurus != "" && entity.thesaurus != null) {
                    return $http.get(entity.thesaurus, {cache: true}).then(function (response) {
                        // $log.info('$scope.targetThesaurus ' +
                        // JSON.stringify($scope.targetThesaurus));
                        // Case Target
                        if (outerIndex == -1) {
                            $scope.targetThesaurus = response.data;
                        }
                        // Case Related Entity
                        else { // if(entityCase == 'related')
                            $scope.thesaurus = response.data;
                        }
                    });
                } else {
                    if (outerIndex == -1) {
                        $scope.targetThesaurus = "";
                    }
                    // Case Related Entity
                    else { // if(entityCase == 'related')
                        $scope.thesaurus = "";
                    }
                }
            } // Closing - if(entity != null)
            else {
                if (outerIndex == -1) {
                    $scope.targetThesaurus = "";
                }
                // Case Related Entity
                else { // if(entityCase == 'related')
                    $scope.thesaurus = "";
                }
            }
        };

        // Used in autocomplete recommendations
        $scope.querySearch = function (query, outerIndex) {
            var results = [];
            if (outerIndex == -1) {
                if ($scope.targetThesaurus != '')
                    results = query ? $scope.targetThesaurus.filter(createFilterFor(query)) : $scope.targetThesaurus;
            } else { // if(entityCase == 'related')
                if ($scope.thesaurus != '')
                    results = query ? $scope.thesaurus.filter(createFilterFor(query)) : $scope.thesaurus;
            }
            return results;
        }

        function createFilterFor(query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(thesaurusItem) {// alert("thesaurus: " +
                // thesaurus);
                return (angular.lowercase(thesaurusItem.name).indexOf(lowercaseQuery) === 0);
            };
        }

        $scope.selectedItemChange = function (item, outerIndex) {
            $log.info('Item in outer-index: ' + outerIndex + ' changed to ' + JSON.stringify(item));
            $log.info($scope.emptyRowModel.relatedChips.length === 0);
        }

        $scope.currRowModel = null;


        $scope.entityResultsCount = 0;

        $scope.showResultsDialog = function (ev, rowModel) {

            // Trying with promise - Start

            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Search process undergoing...'
            };
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);
            var entity = rowModel.selectedTargetEntity !== undefined ? rowModel.selectedTargetEntity : rowModel.selectedRelatedEntity;

            var chips = rowModel.targetChips !== undefined ? rowModel.targetChips : rowModel.relatedChips;
            var searchText = rowModel.searchTargetKeywords !== undefined ? rowModel.searchTargetKeywords : rowModel.relatedEntitySearchText;
            // The search text to feed the query
            var querySearchText = '';

            angular.forEach(rowModel.chips, function (value, key) {
                querySearchText = querySearchText + ' ' + value.name;
            });

            if (rowModel.searchText != null && rowModel.searchText != '') {
                querySearchText = querySearchText + ' ' + rowModel.searchText;
            }

            // Feeding the query with the respective search text

            var entity = rowModel.selectedTargetEntity !== undefined ? rowModel.selectedTargetEntity : rowModel.selectedRelatedEntity;
            console.log("Entity type: " + entity);
            var searchEntityModel = {
                entity: entity.name,
                query: entity.search_query,
                geospatial: entity.entityType,
                searchText: querySearchText,
                fromSearch: $scope.queryFrom,
                relatedChips: chips
                        // fromSearch: 'from <http://ekt-data> from <http://rcuk-data>
                        // from <http://fris-data> from <http://epos-data> from
                        // <http://envri-data>'
            }

            // Getting the query from back-end - Promise

            var updatedQueryModel = '';
            $scope.updatedQueryModelPerPage = '';

            queryService.computeRelatedEntityQuery(searchEntityModel, $scope.credentials.token).then(function (queryResponse) {
                if (queryResponse.status == '200') {

                    updatedQueryModel = {"query": entity.search_query,
                        "format": "application/json"}
                    updatedQueryModel.query = queryResponse.data.query;

                    delete updatedQueryModel.geo_query;
                    delete updatedQueryModel.text_geo_query;


                    // Calling Service to get the count wrt to the query -
                    // Promise
                    queryService.getEntityQueryResultsCount($scope.serviceModel, updatedQueryModel, $scope.credentials.token)
                            .then(function (queryCountResponse) {
                                if (queryCountResponse.status == '200') {
                                    // Holding total number of results
                                    console.log($scope);
                                    $scope.entityResultsCount = queryCountResponse.data.results.bindings[0].count.value;
                                    console.log('$scope.entityResultsCount: ' + $scope.entityResultsCount);

                                    // Change query such that only the first 10 are
                                    // returned
                                    $scope.updatedQueryModelPerPage = Object.assign(updatedQueryModel);

                                    var queryPerPage = "";// angular.copy($scope.updatedQueryModelPerPage.query);
                                    queryPerPage = angular.copy($scope.updatedQueryModelPerPage.query) + ' limit ' + $scope.itemsPerPage.toString() + ' offset ' + ($scope.currentPage - 1).toString();
                                    $scope.updatedQueryModelPerPage.query = queryPerPage;

                                    // Keeping backup of query for easy offset
                                    // change when pressing the next page
                                    // $scope.updatedQueryModelPerPageBackup =
                                    // angular.copy($scope.updatedQueryModelPerPage);
                                    // $scope.updatedQueryModelPerPageBackup.query.replace("offset
                                    // 0","offset !@#NUM#@!");

                                    // Calling service to executing Query - Promise
                                    queryService.getEntityQueryResults($scope.serviceModel, $scope.updatedQueryModelPerPage, $scope.credentials.token)
                                            .then(function (response) {
                                                if (response.status == -1) {
                                                    $scope.message = 'There was a network error. Try again later.';
                                                    $scope.showErrorAlert('Error', $scope.message);
                                                    modalInstance.close();
                                                } else {
                                                    if (response.status == '200') {

                                                        $scope.relatedEntityResults = response.data;

                                                        // Iterating response that
                                                        // doesn't have 'isChecked'
                                                        // element
                                                        for (var i = 0; i < response.data.results.bindings.length; i++) { // Iterating
                                                            // response
                                                            // that
                                                            // doesn't
                                                            // have
                                                            // 'isChecked'
                                                            // element
                                                            console.log(rowModel.selectedRelatedInstanceList);
                                                            if (containedInListBasedOnURI($scope.relatedEntityResults.results.bindings[i], rowModel.selectedRelatedInstanceList, 'uri').contained) {
                                                                $scope.relatedEntityResults.results.bindings[i].isChecked = true;
                                                            }
                                                        }

                                                        modalInstance.close();

                                                        // Used for capturing the
                                                        // current row and thus knowing
                                                        // where to put selected items
                                                        $scope.currRowModel = rowModel;
                                                        $mdDialog.show({
                                                            scope: $scope,
                                                            templateUrl: 'views/dialog/selectFromResults.tmpl.html',
                                                            parent: angular.element(document.body),
                                                            targetEvent: ev,
                                                            preserveScope: true,
                                                            fullscreen: false // Only
                                                                    // for
                                                                    // -xs,
                                                                    // -sm
                                                                    // breakpoints.
                                                        });
                                                    } else if (response.status == '408') {
                                                        $log.info(response.status);
                                                        $scope.message = 'It seems that it takes a lot of time to complete this task! Please redifine your query and try again.';
                                                        $scope.showErrorAlert('Important', $scope.message);
                                                    } else if (response.status == '400') {
                                                        $log.info(response.status);
                                                        $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                                        $scope.showErrorAlert('Error', $scope.message);
                                                        modalInstance.close();
                                                    } else if (response.status == '401') {
                                                        $log.info(response.status);
                                                        modalInstance.close();
                                                        $scope.showLogoutAlert();
                                                        authenticationService.clearCredentials();
                                                    } else {
                                                        $log.info(response.status);
                                                        $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                                        $scope.showErrorAlert('Error', $scope.message);
                                                        modalInstance.close();
                                                    }

                                                } // else close


                                            }, function (error) {
                                                $scope.message = 'There was a network error. Try again later.';
                                                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                                                    data: error
                                                }));
                                                modalInstance.close();
                                            });
                                    // Execute query promise - End
                                } else if (queryCountResponse.status == '408') {
                                    $log.info(response.status);
                                    $scope.message = 'It seems that it takes a lot of time to complete this task! Please redifine your query and try again.';
                                    $scope.showErrorAlert('Important', $scope.message);
                                } else if (queryCountResponse.status == '400') {
                                    $log.info(queryCountResponse.status);
                                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                    $scope.showErrorAlert('Error', $scope.message);
                                    modalInstance.close();
                                } else if (queryCountResponse.status == '401') {
                                    $log.info(queryCountResponse.status);
                                    modalInstance.close();
                                    $scope.showLogoutAlert();
                                    authenticationService.clearCredentials();
                                } else {
                                    $log.info(queryCountResponse.status);
                                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                    $scope.showErrorAlert('Error', $scope.message);
                                    modalInstance.close();
                                }
                            }, function (error) {
                                $scope.message = 'There was a network error. Try again later.';
                                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                                    data: error
                                }));
                                modalInstance.close();
                            });
                    // Count query promise - End
                } else if (queryResponse.status == '400') {
                    $log.info(queryResponse.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                } else if (queryResponse.status == '401') {
                    $log.info(queryResponse.status);
                    modalInstance.close();
                    $scope.showLogoutAlert();
                    authenticationService.clearCredentials();
                } else {
                    $log.info(queryResponse.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                }
            }, function (error) {
                $scope.message = 'There was a network error. Try again later.';
                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                    data: error
                }));
                modalInstance.close();
            }).finally(function () {
                // modalInstance.close();
            });
            // Construct query promise - End
        };

        $scope.closeRelatedEntitySearchResults = function (ev, rowModel) {

            // Check if the list of instances is changed and call service to
            // reload option lists
            if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

            // Hide dialog
            $mdDialog.cancel();
            // Show related entity results panel on respective rowModel
            if (rowModel.shownEntitySearchResults == false && rowModel.selectedRelatedInstanceList.length > 0) {
                rowModel.shownEntitySearchResults = true;
            } else if (rowModel.shownEntitySearchResults == true && rowModel.selectedRelatedInstanceList.length < 1) {
                rowModel.shownEntitySearchResults = false;
            }

            // Respectively handle rowModel.allRelatedSearchResultsIsSelected
            rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
            $scope.handleSelectAllRelatedSearchResults(rowModel);
        }

        $scope.maxSize = 5;
        $scope.currentPage = 1;
        $scope.itemsPerPage = 10;

        // Server-Side Pagination for query
        function getData(rowModel) {
            var pageParams = {
                page: $scope.currentPage,
                itemsPerPage: $scope.itemsPerPage,
            }

            // Preparing new query with offset according to the page number
            // clicked
            var subStrIndex = JSON.stringify($scope.updatedQueryModelPerPage.query).indexOf('offset');
            var subString = JSON.stringify($scope.updatedQueryModelPerPage.query).substr(subStrIndex);
            subString = subString.slice(0, -1);
            console.log(subString);

            $scope.updatedQueryModelPerPage.query = JSON.stringify($scope.updatedQueryModelPerPage.query).replace(subString, 'offset ' + (pageParams.page - 1) * pageParams.itemsPerPage);
            $scope.updatedQueryModelPerPage.query = angular.fromJson($scope.updatedQueryModelPerPage.query);
            console.log($scope.updatedQueryModelPerPage.query);

            queryService.getEntityQueryResults($scope.serviceModel, $scope.updatedQueryModelPerPage, $scope.credentials.token)
                    .then(function (response) {

                        if (response.status == -1) {
                            $scope.message = 'There was a network error. Try again later.';
                            $scope.showErrorAlert('Error', $scope.message);
                        } else {
                            if (response.status == '200') {

                                $scope.relatedEntityResults = response.data;

                                // Iterating response that doesn't have 'isChecked'
                                // element
                                for (var i = 0; i < response.data.results.bindings.length; i++) { // Iterating
                                    // response
                                    // that
                                    // doesn't
                                    // have
                                    // 'isChecked'
                                    // element
                                    if (containedInList($scope.relatedEntityResults.results.bindings[i], rowModel.selectedRelatedInstanceList, true).contained) {
                                        $scope.relatedEntityResults.results.bindings[i].isChecked = true;
                                    }
                                }
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

                        // modalInstance.close();

                    }, function (error) {
                        $scope.message = 'There was a network error. Try again later.';
                        alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                            data: error
                        }));
                        // modalInstance.close();
                    });
        }

        $scope.pageChanged = function () {
            getData($scope.currRowModel);
        };

        // adding or removing selected items from the searched results of the
        // related entity
        // to the list of selected related items
        $scope.changeSelectedRelatedItem = function (item, rowModel, ev) {
            $log.info('currRowModel: ' + $scope.currRowModel.id);

            if (item.isChecked == true) {

                var messageContent = 'There is certain region marked on the map. '
                        + '<br/>'
                        + 'By Choosing to add the selected instances in the query, that region will be cleared out. '
                        + '<br/><br/>'
                        + 'Are you sure you want to continue with this action?';

                // If there region marked, ask permission to delete it
                // (bounding box should not co-exist along with selected
                // instances)
                if (rowModel.boundingBox != undefined) {

                    var confirm = $mdDialog.confirm()
                            .parent(angular.element(document.querySelector('#popupContainerEntityInstanceSearch')))
                            .title('Important Message - Confirmation Required')
                            .htmlContent(messageContent)
                            .ariaLabel('Remove selected bounding box - Confirmation')
                            .targetEvent(ev)
                            .ok('Yes Continue')
                            .cancel('Cancel')
                            .multiple(true);

                    $mdDialog.show(confirm).then(function () { // OK

                        // Clearing bounding box
                        rowModel.backupBoundingBox = angular.copy(rowModel.boundingBox); // Keep
                        // backup
                        // of
                        // bounding
                        // box
                        delete rowModel.boundingBox; // Removing bounding box
                        // from the model
                        $scope.fewPinsInsideBoundingBox = []; // Initializing
                        // array holding
                        // pins inside
                        // bounding box
                        // if they are
                        // few

                        // Add instance in the list

                        // Returned item from the function that checks whether
                        // the item is contained in the list
                        var containedElement = containedInList(item, $scope.currRowModel.selectedRelatedInstanceList, false);
                        if (!containedElement.contained) {
                            $scope.currRowModel.selectedRelatedInstanceList.push(item);
                        }

                    }, function () { // Cancel

                        // Revert flag for search by text and accompanying
                        // indications
                        $scope.showEntitySearchResults(rowModel, false);

                        // Close the entity instance search dialog
                        $mdDialog.cancel();

                    });

                } // (rowModel.boundingBox != undefined) - Ends

                else {
                    // Add instance in the list

                    // Returned item from the function that checks whether the
                    // item is contained in the list
                    var containedElement = containedInList(item, $scope.currRowModel.selectedRelatedInstanceList, false);
                    if (!containedElement.contained) {
                        $scope.currRowModel.selectedRelatedInstanceList.push(item);
                    }
                }

            } else { // if(item.isChecked==false)
                // Returned item from the function that checks whether the item
                // is contained in the list
                var containedElement = containedInList(item, $scope.currRowModel.selectedRelatedInstanceList, true);
                if (containedElement.contained) {
                    $scope.currRowModel.selectedRelatedInstanceList.splice(containedElement.index, 1);
                }
            }
            // $log.info('$scope.relatedEntityResults.results: ' +
            // angular.toJson($scope.relatedEntityResults.results.bindings) );
        }

        $scope.removeSelectedRelatedItem = function (ev, rowModel, itemIndex) {
            $log.info('itemIndex: ' + itemIndex);

            // Delay
            // This is nice but has issues, thus in comment
            // I am applying ng-scope = animateToRemove
            // So if I drop this completely then remove it from the html as well
            /*
             * $timeout( function() { //item.animateToRemove = 'removed-item';
             * rowModel.selectedRelatedInstanceList[itemIndex].animateToRemove =
             * 'removed-item'; });
             */

            // rowModel.selectedRelatedInstanceList[itemIndex].animateToRemove =
            // 'removed-item';
            rowModel.selectedRelatedInstanceList.splice(itemIndex, 1);

            if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

            // Hide related entity search results' panel if there are no items
            // to show
            if (rowModel.selectedRelatedInstanceList.length < 1) {
                rowModel.shownEntitySearchResults = false;

                // Respectively handle
                // rowModel.allRelatedSearchResultsIsSelected
                rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
                $scope.handleSelectAllRelatedSearchResults(rowModel);
            }

        }

        $scope.handleSelectAllRelatedSearchResults = function (rowModel) {

            if (rowModel.allRelatedSearchResultsIsSelected) {
                // Disabled look & feel for the data table
                rowModel.activeRelatedSearchResultsStyle = 'disabled-style';
                // The list of selected becomes empty
                rowModel.selectedRelatedInstanceList = [];
                // All checked boxes becomes un-checked (if available)
                if ($scope.relatedEntityResults != undefined || $scope.relatedEntityResults != null) {
                    angular.forEach($scope.relatedEntityResults.results.bindings, function (value, key) {
                        if (value.isChecked === true) {
                            value.isChecked = false;
                        }
                    });
                }
                rowModel.allRelatedEntitiesSelectedList = [{name: 'Search By Keyword'}];
            } else { // (selected==false)
                // Enabled look & feel for the data table
                rowModel.activeRelatedSearchResultsStyle = 'enabled-style';
            }
        }

        // Determines if an item is contained into a list
        function containedInList(item, list, ignoreIsCheckedProperty) {

            var containedElement = {'contained': false, 'index': -1};

            // var contained = false;
            // var index = -1;

            // Ignoring the 'isChecked property'
            if (ignoreIsCheckedProperty == true) {
                var tempListItem = null; 				// Used in order to
                // delete the isChecked
                // property and compare
                var tempItem = angular.copy(item); 	// Used in order to delete
                // the isChecked property
                // and compare
                for (var i = 0; i < list.length; i++) {

                    tempListItem = angular.copy(list[i]);
                    if (tempListItem.hasOwnProperty('isChecked')) {
                        delete tempListItem.isChecked;
                    }

                    if (tempItem.hasOwnProperty('isChecked')) {
                        delete tempItem.isChecked;
                    }

                    if (angular.toJson(tempListItem) === angular.toJson(tempItem)) {
                        containedElement.contained = true;
                        containedElement.index = i;
                    }
                }
            }

            // Pure compare
            else {// Pure check
                for (var i = 0; i < list.length; i++) {
                    if (angular.toJson(list[i]) === angular.toJson(item)) {
                        containedElement.contained = true;
                        containedElement.index = i;
                    }
                }
            }

            return containedElement;
        }

        // Determines if an item is contained into a list based on URI.value
        function containedInListBasedOnURI(item, list, uriPath) {

            var containedElement = {'contained': false, 'index': -1};

            for (var i = 0; i < list.length; i++) {

                if (item[uriPath].value === list[i][uriPath].value) {
                    containedElement.contained = true;
                    containedElement.index = i;
                }
            }

            return containedElement;
        }

        // Determines if an item is contained into a list
        function containedInListBasedOnFieldPath(item, list, fieldPath) {

            var containedElement = {'contained': false, 'index': -1};

            for (var i = 0; i < list.length; i++) {

                if (item[fieldPath] === list[i][fieldPath]) {
                    containedElement.contained = true;
                    containedElement.index = i;
                }
            }

            return containedElement;
        }

        // Determines if an item is contained into a list
        function containedInListManyTimesBasedOnFieldPathOfDepth2(item, list, itemFieldPath, listFieldPathLevel0, listFieldPathLevel1) {

            var containedElement = {'contained': false, 'index': []};

            var listFieldPathArray = [];
            /*
             * for(var i=0; i<listFieldPath.split(".").length; i++) {
             * listFieldPathArray.push(listFieldPath.split(".")[i]) }
             */
            for (var i = 0; i < list.length; i++) {

                // if(item[itemFieldPath] === list[i][listFieldPath]) {
                if (item[itemFieldPath] === list[i][listFieldPathLevel0][listFieldPathLevel1]) {
                    containedElement.contained = true;
                    containedElement.index.push(i);
                }
            }

            return containedElement;
        }

        // Used when entering a new chip
        $scope.transformChip = function (chip) {
            // If it is an object, it's already a known chip
            if (angular.isObject(chip)) {
                return chip;
            }

            // Otherwise, create a new one
            return {
                // id: 'new',
                name: chip
            }
        }




        // Open dialog for saving into favorites
        $scope.showFavoriteDialog = function (ev) {
            $mdDialog.show({
                scope: $scope,
                templateUrl: 'views/dialog/favoriteForm.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: ev,
                clickOutsideToClose: true,
                preserveScope: true,
                fullscreen: false // Only for -xs, -sm breakpoints.
            });
        }

        // Model to hold current favorite
        $scope.emptyFavoriteModel = {
            username: null,
            title: '',
            description: '',
            queryModel: {
                targetModel: null,
                relatedModels: null,
                namegraphs: []
            },
            id: null
        };

        $scope.favoriteModel = angular.copy($scope.emptyFavoriteModel);

        // Save into favorites
        $scope.saveIntoFavorites = function () {

            // Updating model holding current favorite
            $scope.favoriteModel.username = $sessionStorage.userProfile.userId;
            $scope.favoriteModel.queryModel.targetModel = $scope.targetModel;
            $scope.favoriteModel.queryModel.relatedModels = $scope.rowModelList;
            $scope.favoriteModel.queryModel.namegraphs = $scope.namegraphs;
            $scope.favoriteModel.queryModel.configuration = angular.copy($scope.configuration);

            // If already favorite, then update it
            if ($scope.currentFavorite.dbTableId != undefined || $scope.currentFavorite.dbTableId != null)
                $scope.favoriteModel.id = $scope.currentFavorite.dbTableId

            queryService.saveIntoFavorites(angular.toJson($scope.favoriteModel), $scope.credentials.token)
                    .then(function (response) {

                        if (response.status == '200') {
                            if (response.data.dbStatus == 'success') {
                                // Hide Dialog
                                $mdDialog.cancel();
                                // Display msg
                                $mdToast.show(
                                        $mdToast.simple()
                                        .textContent('The Query has been stored successfully!')
                                        .position('top right')
                                        .parent(angular.element('#mainContent'))
                                        .hideDelay(3000)
                                        );
                                // If not already favorite, then make it favorite
                                if ($scope.currentFavorite.dbTableId == undefined || $scope.currentFavorite.dbTableId == null) {
                                    $scope.currentFavorite.itIsFavorite = true;
                                    $scope.currentFavorite.dbTableId = response.data.generatedId;
                                    $scope.favoriteTitle = $scope.favoriteModel.title;
                                }

                                // Setting flag that makes clear that the query is
                                // not under construction any more
                                // (used to allow leaving without confirmation since
                                // it is saved)
                                homeStateConfirmService.setQueryUnderConstruction(false);
                                $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
                                // favorite
                                // as
                                // changed
                                // (if
                                // favorite)
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

        }

        // Hide Favorite Dialog
        $scope.closeFavoriteDialog = function () {
            $mdDialog.cancel();
        }

        // Removing the current queryModel from the database (uses the
        // currentFavorite.dbTableId)
        $scope.removeCurrentQueryModelFromFavorites = function (ev) {
            if ($scope.currentFavorite.dbTableId != null) {

                // Ask before removing from favorites
                var confirm = $mdDialog.confirm({
                    onComplete: function afterShowAnimation() {
                        var $dialog = angular.element(document.querySelector('md-dialog'));
                        var $actionsSection = $dialog.find('md-dialog-actions');
                        var $cancelButton = $actionsSection.children()[0];
                        var $confirmButton = $actionsSection.children()[1];
                        angular.element($confirmButton).addClass('md-raised md-warn');
                        angular.element($cancelButton).addClass('md-raised');
                    }
                })
                        .title('Warning Message')
                        .htmlContent('Are you sure you want to remove this query from your favorites?')
                        .ariaLabel('Confirmation')
                        .targetEvent(ev)
                        .ok('Yes Proceed')
                        .cancel('Cancel');

                $mdDialog.show(confirm).then(function () { // OK
                    queryService.removeFromFavoritesById($scope.currentFavorite, $scope.credentials.token)
                            .then(function (response) {
                                if (response.status == '200') {
                                    if (response.data.dbStatus == 'success') {
                                        // Display msg
                                        $mdToast.show(
                                                $mdToast.simple()
                                                .textContent('The Query has been removed from your favorites!')
                                                .position('top right')
                                                .parent(angular.element('#mainContent'))
                                                .hideDelay(3000)
                                                );
                                        $scope.currentFavorite.itIsFavorite = false;
                                        $scope.currentFavorite.dbTableId = null;
                                        $scope.favoriteModel = angular.copy($scope.emptyFavoriteModel);
                                        // Making $sessionStorage.selectedQueryModel
                                        // null, to free up memory
                                        $sessionStorage.selectedFavoriteModel = null;

                                        $scope.finalResults = {};
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
                    //
                });


            }
        }

        $scope.applyReset = function (ev) {
            var confirm = $mdDialog.confirm(
                    {
                        onComplete: function afterShowAnimation() {
                            var $dialog = angular.element(document.querySelector('md-dialog'));
                            var $actionsSection = $dialog.find('md-dialog-actions');
                            var $cancelButton = $actionsSection.children()[0];
                            var $confirmButton = $actionsSection.children()[1];
                            angular.element($confirmButton).addClass('md-raised md-warn');
                            angular.element($cancelButton).addClass('md-raised');
                        }
                    })
                    .title('Warning Message')
                    .htmlContent('Are you sure you want to reset the query constructed so far?')
                    .ariaLabel('Target Entity Selection - No longer Available')
                    .targetEvent(ev)
                    .ok('Yes Proceed')
                    .cancel('Cancel');

            $mdDialog.show(confirm).then(function () { // OK
                resetWholeQueryModel(false);

                // Resetting favorite related options
                $scope.currentFavorite.itIsFavorite = false;
                $scope.currentFavorite.dbTableId = null;
                $scope.favoriteModel = angular.copy($scope.emptyFavoriteModel);
                // Making $sessionStorage.selectedQueryModel null, to free up
                // memory
                $sessionStorage.selectedFavoriteModel = null;

                $scope.finalResults = {};
            }, function () { // Cancel
                // Do nothing
            });
        }

        // Resets the whole query model
        function resetWholeQueryModel(openTreeMenu) {

            // Close sideNav for selected result item (if opened)
            if ($mdSidenav('resultItemInfoSidenav').isOpen())
                $mdSidenav('resultItemInfoSidenav').close();

            // Initializing empty row model (new instance)
            $scope.emptyRowModel = angular.copy($scope.initEmptyRowModel);
            $scope.rowModelList = [$scope.emptyRowModel];

            // Handling favorites
            if ($scope.currentFavorite.itIsFavorite) {
                // Re-retrieve all entities
                constructQueryFrom($scope.namegraphs);
            }

            initAllEntities($scope.queryFrom, false);

            // Resetting target model
            $scope.targetModel.selectedTargetEntity = null;
            $scope.targetModel.backupSelectedTargetEntity = null;
            // $scope.targetModel.targetEntities = $scope.allAvailableEntities;
            $scope.targetModel.searchTargetKeywords = '';
            $scope.targetModel.selectedTargetRecomentation = null;
            $scope.targetModel.targetChips = [];
            $scope.targetModel.backupTargetChips = [];
            $scope.targetModel.availableFilterExpressions = [{expression: 'OR'}, {expression: 'AND'}];

            // console.log($scope.rowModelList);
            // Setting select inputs untouched
            // $scope.searchForm.$setPristine();
            $scope.searchForm['targetEntityInput'].$setUntouched();
            var rowModelId = $scope.rowModelList[0].id;
            if ($scope.searchForm['relatedEntityInput_' + rowModelId] != null)
                $scope.searchForm['relatedEntityInput_' + rowModelId].$setUntouched();
            if ($scope.searchForm['relationInput_' + rowModelId] != null)
                $scope.searchForm['relationInput_' + rowModelId].$setUntouched();

            // Open tree menu again
            // change : not opon tree model
            if (openTreeMenu) {
                // Initial value for the flag
                $scope.treeMenuIsOpen = true;
                // Open treeMenu
                $scope.toggleTreeMenu();
            }

            // Keep first copy of namedgraphs
            $scope.namegraphsCopy = angular.copy($scope.namegraphs);

            // Setting flag that makes clear that the query is not under
            // construction any more
            // (used to allow leaving without confirmation)
            homeStateConfirmService.setQueryUnderConstruction(false);
            $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
            // favorite
            // as
            // changed
            // (if
            // favorite)

            // Display msg
            $mdToast.show(
                    $mdToast.simple()
                    .textContent('The Query has been reset!')
                    .position('top right')
                    .parent(angular.element('#mainContent'))
                    .hideDelay(3000)
                    );

        }

        // The ui-codemirror option
        $scope.cmOption = {
            lineNumbers: true,
            lineWrapping: true,
            indentWithTabs: true,
            // fixedGutter: true,
            // autoRefresh: true,
            readOnly: true,
            // readOnly: 'nocursor',
            mode: 'sparql'
        };

        $scope.fullScreenStyleForDialog = 'non-full-screen-dialog';

        $scope.makeSparqlObservationInFullScreen = function () {
            $scope.fullScreenStyleForDialog = 'full-screen-dialog';
            $scope.refreshCodemirror = true;
            $timeout(function () {
                $scope.refreshCodemirror = false;
            }, 100);
        }

        $scope.exitSparqlObservationFromFullScreen = function () {
            $scope.fullScreenStyleForDialog = 'non-full-screen-dialog';
            $scope.refreshCodemirror = true;
            $timeout(function () {
                $scope.refreshCodemirror = false;
            }, 100);
        }

        $scope.observeSparql = function (ev) {

            $mdDialog.show({
                scope: $scope,
                templateUrl: 'views/dialog/sparqlObserve.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: ev,
                clickOutsideToClose: true,
                onComplete: function () {
                    // Development purpose
                    var model = {
                        queryFrom: $scope.queryFrom,
                        queryModel: {
                            targetModel: angular.copy($scope.targetModel),
                            relatedModels: angular.copy($scope.rowModelList)
                        }
                    }

                    // Delete Useless for the back-end properties, occupying a
                    // lot of volume

                    // Target
                    delete model.queryModel.targetModel.backupSelectedTargetEntity;
                    delete model.queryModel.targetModel.targetEntities;

                    // Related Entity List (whole (model.queryModel))
                    for (var i = 0; i < model.queryModel.relatedModels.length; i++) {
                        deleteUselessForBackEndRelatedProperties(model.queryModel.relatedModels[i], true);
                    }

                    computeFinalQuery(angular.toJson(model));

                },
                // fullscreen: true,
                preserveScope: true,
                fullscreen: false // Only for -xs, -sm breakpoints.
            })
                    .then(function (data) {
                        console.log("then");
                    }, function (err) {
                        $scope.status = 'You cancelled the dialog.';
                    }).finally(function () {

            });

        }

        function computeFinalQuery(searchEntityModel) {
            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Search process undergoing...'
            };
            // Remove temo modal, Modal not closes bug
            // var modalInstance = modalService.showModal(modalDefaults,
            // modalOptions);

            queryService.computeFinalSearchQuery(searchEntityModel, $scope.credentials.token).then(function (queryResponse) {
                if (queryResponse.status == '200') {
                    $scope.finalQuery = queryResponse.data.query;

                    // Trick to make line-numbers render correctly
                    $scope.refreshCodemirror = true;
                    $timeout(function () {
                        $scope.refreshCodemirror = false;
                    }, 100);
                    // modalInstance.close();
                } else if (queryResponse.status == '400') {
                    $log.info(queryResponse.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    // modalInstance.close();
                } else if (queryResponse.status == '401') {
                    $log.info(queryResponse.status);
                    modalInstance.close();
                    $scope.showLogoutAlert();
                    authenticationService.clearCredentials();
                } else {
                    $log.info(queryResponse.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    // modalInstance.close();
                }
            }, function (error) {
                $scope.message = 'There was a network error. Try again later.';
                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                    data: error
                }));
                // modalInstance.close();
            });

        }



        $scope.closeSparqlObserve = function () {
            // Hide dialog
            $mdDialog.cancel();
        }

        // Solves the error "Controller 'mdSelectMenu', required by directive
        // 'mdOption', can't be found!"
        $scope.canShowConfigDialog = true;

        // Opens dialog for the configuration
        $scope.openConfigurationDialog = function (ev) {

            $scope.canShowConfigDialog = true;

            // Using this for configuration only
            $scope.allAvailableEntities = angular.copy($scope.allEntities);

            $mdDialog.show({
                scope: $scope,
                templateUrl: 'views/dialog/configuration.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: ev,
                autoWrap: false, // Solves the error "Controller
                // 'mdSelectMenu', required by directive
                // 'mdOption', can't be found!"
                preserveScope: true,
                fullscreen: false // Only for -xs, -sm breakpoints.
            });
        }

        $scope.closeConfigurationDialog = function () {

            // Solves the error "Controller 'mdSelectMenu', required
            // by directive 'mdOption', can't be found!"
            $scope.canShowConfigDialog = false;

            $scope.configGeneralHelpShown = false; // Hide help if shown
            $scope.configEntityOptionsHelpShown = false; // Hide help if
            // shown

            if ($scope.targetModel.selectedTargetEntity != null) {

                var containedInListObject = containedInListBasedOnFieldPath($scope.targetModel.selectedTargetEntity, $scope.configuration.targetEntity.excludedEntities, 'name');
                if (containedInListObject.contained) {
                    var message = 'The entity '
                            + '<code>' +
                            $scope.configuration.targetEntity.excludedEntities[containedInListObject.index].name
                            + '</code> '
                            + 'cannot be set as excluded, since it has already been selected as target entity. '
                            + 'Hence, it will be removed from the list of excluded entities.';
                    $mdDialog.show(
                            $mdDialog.alert()
                            .parent(angular.element(document.querySelector('#popupContainerOnConfiguration')))
                            .title('Warning')
                            .htmlContent(message)
                            .ariaLabel('Logout Message')
                            .ok('OK')
                            .multiple(true)
                            ).finally(function () {

                        // Removing already selected target entity from excluded
                        // list for target entities
                        $scope.configuration.targetEntity.excludedEntities.splice(containedInListObject.index, 1);

                        $mdDialog.cancel(); // Hide dialog
                        // Initializing AllEntities
                        initAllEntities($scope.queryFrom, false);
                    });
                } else {
                    $mdDialog.cancel(); // Hide dialog
                    initAllEntities($scope.queryFrom, false);
                }
            } else {
                $mdDialog.cancel(); // Hide dialog
                initAllEntities($scope.queryFrom, false);
            }
        }
        /*
         * $scope.dynamicPopover = { levelLimit: { content: '', templateUrl:
         * 'levelLimitTemplate', title: 'Level Limit - Help' }, degreeLimit: {
         * content: '', templateUrl: 'degreeLimitTemplate', title: 'Degree Limit -
         * Help' }, bothAndOr: { content: '', templateUrl: 'bothAndOrTemplate',
         * title: 'Regular Expr. - Help' } };
         */
        $scope.applySearch = function () {

            // Setting the first page to be the current one
            $scope.currentPage = 1;

            // Development purpose
            var model = {
                queryFrom: $scope.queryFrom,
                queryModel: {
                    targetModel: angular.copy($scope.targetModel),
                    relatedModels: angular.copy($scope.rowModelList)
                }
            }

            // Delete Useless for the back-end properties, occupying a lot of
            // volume

            // ----------- model.queryModel:

            // Target
            delete model.queryModel.targetModel.backupSelectedTargetEntity;
            delete model.queryModel.targetModel.targetEntities;

            // Related Entity List (whole (model.queryModel))
            for (var i = 0; i < model.queryModel.relatedModels.length; i++) {
                deleteUselessForBackEndRelatedProperties(model.queryModel.relatedModels[i], true);
            }

            $log.info(angular.toJson(model));
            // $scope.showErrorAlert('Info', 'Running the query will be
            // available in the final version. For the moment only
            // construction-related functionality is possible.');
            retrieveFinalResults(angular.toJson(model));
        };

        $scope.finalResults = {};
        $scope.finalResultsMaxCountReached = false;

        function retrieveFinalResults(searchEntityModel) {

            // Trying with promise - Start

            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Search process undergoing...'
            };
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            queryService.computeFinalSearchQuery(searchEntityModel, $scope.credentials.token).then(function (queryResponse) {
                if (queryResponse.status == '200') {
                    console.log($scope.targetModel);
                    var model = {
                        queryModel: {
                            targetModel: angular.copy($scope.targetModel),
                            relatedModels: angular.copy($scope.rowModelList)
                        }
                    }

                    var params = {
                        format: "application/json",
                        query: queryResponse.data.query, // + ' limit 300',
                        // // final Search
                        // Query
                        itemsPerPage: $scope.itemsPerPage,
                        userProfile: $scope.userProfile, 
                        // only
                        // (to store results
                        // into a temp
                        // namedgraph)
                        targetModel: angular.copy($scope.targetModel),
                        relatedModels: angular.copy($scope.rowModelList)
                    }
                    // Calling service to executing Query - Promise
                    queryService.getFinalQueryResults(params, model, $scope.credentials.token)
                            .then(function (response) {

                                if (response.status == -1) {
                                    $scope.message = 'There was a network error. Try again later.';
                                    $scope.showErrorAlert('Error', $scope.message);
                                } else {
                                    if (response.status == '200') {
                                        $scope.finalResults = response.data;
                                        $scope.finalResultsMaxCountReached = false; // Resetting
                                        // initial
                                        // value
                                        // If the max limit of returned results has
                                        // been reached
                                        if ($scope.finalResults.totalItems == $scope.serviceModel.maxResultCountLimit)
                                            $scope.finalResultsMaxCountReached = true;
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
                                        $scope.message = 'Please try a different input!';
                                        $scope.showErrorAlert('No results!', $scope.message);
                                    }

                                } // else close

                            }, function (error) {
                                $scope.message = 'There was a network error. Try again later.';
                                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                                    data: error
                                }));
                            }).finally(function () {
                        modalInstance.close();
                    });
                    // Execute query promise - End
                } else if (queryResponse.status == '400') {
                    $log.info(queryResponse.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                } else if (queryResponse.status == '401') {
                    $log.info(queryResponse.status);
                    modalInstance.close();
                    $scope.showLogoutAlert();
                    authenticationService.clearCredentials();
                } else {
                    $log.info(queryResponse.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                }
            }, function (error) {
                $scope.message = 'There was a network error. Try again later.';
                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                    data: error
                }));
                modalInstance.close();
            });
            // Construct query promise - End
        }
        ;


        // Put them into the configuration
        $scope.currentPage = 1;
        $scope.itemsPerPage = 30;// 100;


        // Server-Side Pagination for query
        $scope.getFinalReusltsForData = function () {
            var pageParams = {
                page: $scope.currentPage,
                itemsPerPage: $scope.itemsPerPage
            }

            queryService.getFinalQueryResultsForPage(pageParams, $scope.credentials.token)
                    .then(function (response) {
                        $scope.finalResults = response.data;
                    }, function (error) {
                        $scope.message2 = 'There was a network error. Try again later.';
                        alert("failure message: " + $scope.message2 + "\n" + JSON.stringify({
                            data: error
                        }));
                    });
        }
        // Handling clicking on row of final results, by resolving
        // the item (through the URI) in a new tab
        $scope.handleClickedRowFromFinalResults = function (uri) {
            $window.open(uri, '_blank');
        }

        // Pretty Print

        // Closing result item info sidenav
        $scope.closeResultItemInfo = function () {
            $scope.resultItemNavHistory = [];

            $scope.previousResultItemNavHistory = {};
            // Component lookup should always be available since we are not
            // using `ng-if`
            $mdSidenav('resultItemInfoSidenav').close()
                    .then(function () {
                        // $log.debug("close LEFT is done");
                    });

        };

        // The currently selected result item
        $scope.currSelectedResultItem = {};

        // The history of the selected result item
        // (only URI and type properties)
        $scope.resultItemNavHistory = [];

        // The previous item in the history list for the selected result item
        // (only URI and type properties)
        $scope.previousResultItemNavHistory = {};

        // Used for holding settings regarding "observing a result item"
        $scope.resultsModel = {
            viewStyleOptions: [
                {name: 'side Navigation', value: 'results-sidenav-style'},
                {name: 'sticky Navigation', value: 'results-sticky-style'}
            ],
            selectedViewStyle: {
                name: 'side Navigation',
                value: 'results-sticky-style'
            }
        }

        $scope.setResultViewStyle = function (viewStyle) {
            $scope.resultsModel.selectedViewStyle = viewStyle;
        }

        $scope.handlePreviousSelectedResultItem = function () {
            var itemUri = angular.copy($scope.resultItemNavHistory[$scope.resultItemNavHistory.length - 2].uri);
            var entityName = angular.copy($scope.resultItemNavHistory[$scope.resultItemNavHistory.length - 2].entityName);
            var title = angular.copy($scope.resultItemNavHistory[$scope.resultItemNavHistory.length - 2].title);

            $scope.handleSelectedResultItem(title, itemUri, entityName, false, true); // True
            // stands
            // that this
            // is a
            // "back
            // from
            // history"
            // call
        }

        // Called when clicking on a result item (to toggle sidenav)
        // rootUri: boolean - means that this function is called by clicking on
        // an result item and not on
        // a related entity within info for a result item
        // backFromHistory: boolean - means that this function is called when
        // clicking the back button
        // (to observe the previous URI)
        $scope.handleSelectedResultItem = function (title, itemUri, entityName, rootUri, backFromHistory) {



            // Checking if this URI is an external link

            // If it is external link then open the link in a new tab
            // uriPrefix is usually something like "http://139.91.183.70"
            // and is used for determining whether the URI starts with this
            // denoting that in that case it is an internal URI that can be
            // resolved through the internal navigation UI of the final results
            //

            // NEW ADDED IN COMMENT THE BELOW TWO
            // if (!itemUri.startsWith($scope.serviceModel.uriPrefix))
            // $window.open(itemUri, '_blank');

            // Otherwise use the internal resolver
            // else { //if(itemUri.startsWith("http://139.91.183.70"))

            // Parameters for the service to call
            console.log("itemUri::  " + itemUri);
            var params = {
                fromSearch: $scope.queryFrom,
                entityUri: itemUri,
                entityName: entityName,
                title: title
            }

            // Modal Options
            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Retrieving Information Data for this entity...'
            };

            // Start modal
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            // Calling Service to retrieve item's details
            queryService.retrieveEntityInfo(params, $scope.credentials.token).then(function (response) {
                if (response.status == '200') {
                    $scope.currSelectedResultItem = response.data;
                    // console.log("currSelectedResultItem: " +
                    // angular.toJson($scope.currSelectedResultItem));
                    modalInstance.close();
                    $mdSidenav('resultItemInfoSidenav').open();

                    // Handling history

                    // Case: Click from the results

                    if (rootUri) {
                        // Initializing history in case of clicking on a result
                        // item
                        $scope.resultItemNavHistory = [];
                        // Initializing previous item from the history of the
                        // selected result item
                        $scope.previousResultItemNavHistory = {};
                    }

                    // Case: Click either from the results or from links inside
                    // the selected result
                    if (!backFromHistory) { // Adding URI into history
                        $scope.resultItemNavHistory.push(
                                {
                                    uri: itemUri,
                                    entityName: entityName,
                                    title: response.data.title
                                }
                        );

                        // Case: Click from the "Back to previous entity" button
                    } else {// Removing URI from history
                        console.log("Removing URI")
                        $scope.resultItemNavHistory.splice(-1, $scope.resultItemNavHistory.length - 1);
                    }
                    // Case: Click only from links inside the selected result
                    if (!rootUri && $scope.resultItemNavHistory.length > 1) {
                        // Holding previous

                        $scope.previousResultItemNavHistory = {
                            uri: angular.copy($scope.resultItemNavHistory[$scope.resultItemNavHistory.length - 2].uri),
                            entityName: angular.copy($scope.resultItemNavHistory[$scope.resultItemNavHistory.length - 2].entityName),
                            title: angular.copy($scope.resultItemNavHistory[$scope.resultItemNavHistory.length - 2].title),

                        };

                    }

                } else if (response.status == '400') {
                    $log.info(response.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                } else if (response.status == '401') {
                    $log.info(response.status);
                    modalInstance.close();
                    $scope.showLogoutAlert();
                    authenticationService.clearCredentials();
                } else {
                    $log.info(response.status);
                    $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                    $scope.showErrorAlert('Error', $scope.message);
                    modalInstance.close();
                }
            }, function (error) {
                $scope.message = 'There was a network error. Try again later.';
                alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                    data: error
                }));
            }).finally(function () {
                console.log($scope.resultItemNavHistory);
            });

            // } // else close -
            // //if(itemUri.startsWith("http://139.91.183.70"))

        }































































        $scope.showMapForRelatedResultsDialog = function (ev, rowModel) {
            console.log(rowModel);
            // Keep Backup for selected pins and bounding box set
            rowModel.backupSelectedRelatedInstanceList = angular.copy(rowModel.selectedRelatedInstanceList);
            rowModel.backupBoundingBox = angular.copy(rowModel.boundingBox); // Keep
            // backup
            // of
            // bounding
            // box

            // Initializing array holding the pins when they are few (defined
            // by:
            // configuration.relatedEntity.map.minResoultCountForAutoSelectingPinsOnDrawingBox)
            $scope.fewPinsInsideBoundingBox = [];

            // Used for capturing the current row and thus knowing where to put
            // selected items
            $scope.currRowModel = rowModel;
            $mdDialog.show({
                scope: $scope,
                templateUrl: 'views/dialog/selectFromMap.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: ev,
                // clickOutsideToClose:true,
                onComplete: function () {
                    // Loading Map
                    loadMapForRelatedEntity(rowModel);

                    // If the model has bounding box, then display it on the map
                    if (rowModel.boundingBox != undefined) {

                        // Enabling button that clears bounding box
                        document.getElementById("clearBoundingBoxButtonId").disabled = false;

                        // The bounding box to draw
                        $scope.currBoundingBoxFeature = new ol.Feature({
                            geometry: new ol.geom.Polygon([
                                [
                                    [parseFloat(rowModel.boundingBox.west), parseFloat(rowModel.boundingBox.north)],
                                    [parseFloat(rowModel.boundingBox.west), parseFloat(rowModel.boundingBox.south)],
                                    [parseFloat(rowModel.boundingBox.east), parseFloat(rowModel.boundingBox.south)],
                                    [parseFloat(rowModel.boundingBox.east), parseFloat(rowModel.boundingBox.north)],
                                    [parseFloat(rowModel.boundingBox.west), parseFloat(rowModel.boundingBox.north)]
                                ]
                            ])
                        });
                        /*
                         * // The style of the pre-drawn bounding box var
                         * boundingBoxStyle = new ol.style.Style({ stroke: new
                         * ol.style.Stroke({ color: 'blue', width: 1 }), fill:
                         * new ol.style.Fill({ color: 'rgba(0, 0, 255, 0.1)' })
                         * }); // Stylng pre-drawn bounding box
                         * $scope.currBoundingBoxFeature.setStyle(boundingBoxStyle);
                         */

                        // Holding Coordinates
                        var coordinateStr = $scope.currBoundingBoxFeature.clone().getGeometry().getCoordinates();

                        // Change coordinate systems to display on the map
                        $scope.currBoundingBoxFeature.getGeometry().transform('EPSG:4326', 'EPSG:3857');

                        // Handle the coordinates
                        $scope.convertCoordinatesToJson(coordinateStr);  // (loaded
                        // on
                        // init)

                    } else { // (rowModel.boundingBox == undefined)
                        // Capturing the whole map
                        $scope.coordinatesRegion.north = 90.0000;
                        $scope.coordinatesRegion.south = -90.0000;
                        $scope.coordinatesRegion.west = -180.0000;
                        $scope.coordinatesRegion.east = 180.0000;
                    }

                    // Calling the service to load results on the map, when
                    // their count is less than a max allowed number
                    $scope.retrieveGeoData(true, false, ev); 	// First boolean
                    // stands for
                    // ignoring
                    // "maxResoultCountForShowingPinsOnInit"
                    // (when drawing
                    // rectangle)
                    // and boundingBoxAction is a boolean denoting that this
                    // action will be applied due to
                    // bounding box drawing action
                    // Second boolean stands for drawingBox action (if we are
                    // drawing a bounding box)
                },
                // fullscreen: true,
                preserveScope: true,
                fullscreen: false // Only for -xs, -sm breakpoints.
            })
                    .then(function (data) {
                        console.log("then");
                    }, function (err) {
                        $scope.status = 'You cancelled the dialog.';
                    }).finally(function () {

            });
        }

        function loadMapForRelatedEntity(rowModel) {

            // Starting with map in related entity

            var type = rowModel.selectedTargetEntity !== undefined ? rowModel.selectedTargetEntity.name : rowModel.selectedRelatedEntity.name;
            $scope.pins = [{
                    type: type,
                    selectedImg: "../aqub/images/Map-Marker-Marker-Inside-Pink-icon.png",
                    unselectedImg: "../aqub/images/Map-Marker-Marker-Outside-Pink-icon.png"
                }]


            var mousePositionControl = new ol.control.MousePosition({
                coordinateFormat: ol.coordinate.createStringXY(4),
                projection: 'EPSG:4326', // 3857 //4326
                // comment the following two lines to have the mouse position
                // be placed within the map.
                className: 'custom-mouse-position',
                target: document.getElementById('mouse-position'),
                undefinedHTML: '&nbsp;'
            });


            var logoElement = document.createElement('a');
            logoElement.href = 'https://www.../';
            logoElement.target = '_blank';

            var logoImageElement = document.createElement('img');
            logoImageElement.src = 'https://www....';
            logoImageElement.style.fontSize = '200%';

            logoElement.appendChild(logoImageElement);

            var explainPinsElement = document.createElement('span');

            // Content of attribution (displayed along with the logo)
            angular.forEach($scope.pins, function (pin) {
                var pinLabelElement = document.createElement('span');
                pinLabelElement.style.fontSize = '200%';
                pinLabelElement.innerHTML = pin.type + ':';
                var pinImgElement = document.createElement('img');
                pinImgElement.src = pin.unselectedImg;
                pinImgElement.style.fontSize = '200%';

                explainPinsElement.appendChild(pinLabelElement);
                explainPinsElement.appendChild(pinImgElement);

            });

            var attribution = new ol.Attribution({
                html: explainPinsElement.innerHTML
            });

            $scope.map = new ol.Map({
                controls: ol.control.defaults({
                    attributionOptions: /** @type {olx.control.AttributionOptions} */ ({
                        collapsible: true,
                        tipLabel: 'Information regarding the pins on the map'
                    })
                })
                        .extend([mousePositionControl]),
                // .extend([mousePositionControl, new searchByBoundingBox()]),
                // .extend([new ol.control.FullScreen()]),
                layers: [
                    new ol.layer.Tile({
                        source: new ol.source.OSM({
                            attributions: [
                                attribution// ,
                                        // new ol.Attribution({ html: '<br/>© Google'
                                        // }),
                                        // new ol.Attribution({ html: '<a
                                        // href="https://developers.google.com/maps/terms">Terms
                                        // of Use.</a>' })
                            ],
                            // url:
                            // "https://maps.wikimedia.org/osm-intl/{z}/{x}/{y}.png",
                            // // Not bad
                            // url:
                            // "http://{a-c}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
                            // // Too gray
                            // url:
                            // "http://{a-c}.tile.thunderforest.com/outdoors/{z}/{x}/{y}.png",
                            // // API Key Required
                            // url:
                            // "http://tile2.opencyclemap.org/transport/{z}/{x}/{y}.png",
                            // // API Key Required
                            // url:
                            // "http://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png",
                            // // Default (no need to enter it at all)
                            // url:
                            // "http://mt{0-3}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
                            // // Google needs to uncoment attributions
                            // url: "none",
                            crossOrigin: null
                        })
                    })
                ],
                target: 'map',
                view: new ol.View({
                    center: [1000000, 4912205], // Default is [0, 0]
                    zoom: 2, // Default is 2 // was 5
                    minZoom: 2,
                    maxZoom: 19
                }), // To hide logo use:
                logo: logoElement 	// document.createElement('span')
            });

            // Bounding Box

            // Setting Bounding Box in the query (button) - Starts

            // Button for setting bounding box
            var boundingBoxIconElement = document.createElement('img');
            boundingBoxIconElement.src = '../aqub/images/boundingBox.svg';

            var boundingBoxButton = document.createElement('button');
            boundingBoxButton.title = "Set bounding box";

            boundingBoxButton.appendChild(boundingBoxIconElement);

            // Array to hold the drawn bounding boxes (even though I only allow
            // one at a time)
            var boundingBoxFeatures = new ol.Collection();

            // Global source for drawn bounding box such that it is easily
            // cleared when necessary
            $scope.boxSource = new ol.source.Vector({
                wrapX: false,
                features: boundingBoxFeatures
            });

            // Interaction for drawing the bounding box
            var drawBoundingBox = new ol.interaction.Draw({
                source: $scope.boxSource,
                type: 'Circle',
                geometryFunction: ol.interaction.Draw.createBox()
            });

            // Vector for holding the drawn bounding boxes (allowing one per
            // time)
            var boxVectorLayer = new ol.layer.Vector({
                source: $scope.boxSource
            });

            $scope.map.addLayer(boxVectorLayer);	// Adding Layer for box

            // Handling bounding box drawn
            var setBoundingBoxInQuery = function (e) {
                // Dealing with the map
                $scope.polyFeatures.clear();
                $scope.pointFeatures.clear();
                $scope.select.getFeatures().clear();
                $scope.boxSource.clear(); // Clearing drawn bounding box
                $scope.map.addInteraction(drawBoundingBox); // Initiating
                // interaction for
                // drawing
            };

            // When the drawing for the bounding box finish
            drawBoundingBox.on('drawend', function (e) {
                $scope.map.removeInteraction(drawBoundingBox);

                // Enabling button that clears bounding box
                document.getElementById("clearBoundingBoxButtonId").disabled = false;

                var boxGeometry = e.feature.getGeometry().clone();
                var coordinateStr = boxGeometry.transform('EPSG:3857', 'EPSG:4326').getCoordinates();
                // console.log('coordinateStr: ' + coordinateStr);
                $scope.convertCoordinatesToJson(coordinateStr);

                // Calling the service to retrieve the pins
                $scope.retrieveGeoData(false, true, null); 	// First boolean
                // stands for
                // ignoring
                // "maxResoultCountForShowingPinsOnInit"
                // (when drawing
                // rectangle)
                // and boundingBoxAction is a boolean denoting that this action
                // will be applied due to
                // bounding box drawing action
                // Second boolean stands for drawingBox action (if we are
                // drawing a bounding box)

            });

            boundingBoxButton.addEventListener('click', setBoundingBoxInQuery, false);

            var boundingBoxElement = document.createElement('div');
            boundingBoxElement.className = 'boundingBoxInQuery ol-unselectable ol-control';
            boundingBoxElement.appendChild(boundingBoxButton);
            /*
             * var boundingBoxInQueryControl = new ol.control.Control({ element:
             * boundingBoxElement });
             * $scope.map.addControl(boundingBoxInQueryControl);
             */

            // Button for clearing bounding box

            var clearBoundingBoxIconElement = document.createElement('img');
            clearBoundingBoxIconElement.src = '../aqub/images/clearBoundingBox.svg';

            var clearBoundingBoxButton = document.createElement('button');
            clearBoundingBoxButton.id = 'clearBoundingBoxButtonId';
            clearBoundingBoxButton.title = "Clear currently defined bounding box";
            clearBoundingBoxButton.disabled = 'true';

            clearBoundingBoxButton.appendChild(clearBoundingBoxIconElement);

            var clearBoundingBoxInQuery = function (e) {
                $scope.polyFeatures.clear();
                $scope.pointFeatures.clear();
                $scope.select.getFeatures().clear();
                $scope.boxSource.clear(); // Clearing drawn bounding box
                delete rowModel.boundingBox; // Removing bounding box from
                // the model
                $scope.fewPinsInsideBoundingBox = []; // Initializing array
                // holding pins inside
                // bounding box if they
                // are few
                rowModel.boundingBoxResultsCount = 0;

                // Disabling button that clears bounding box
                document.getElementById("clearBoundingBoxButtonId").disabled = true;

                // Re-Retrieving selected instances
                if ($scope.configuration.relatedEntity.map.alwaysShowPinsForSelectedInstances)
                    handleGeoResultsForMap(rowModel.selectedRelatedInstanceList, true); // true
                // stands
                // for
                // selecting
                // them
                // all


                // Display message informing user that bounding box has been
                // removed
                $mdToast.show(
                        $mdToast.simple()
                        .textContent('Bounding box has been removed!')
                        .position('top right')
                        .parent(angular.element('#mapDialogMainContent'))
                        .hideDelay(3000)
                        );

            };

            clearBoundingBoxButton.addEventListener('click', clearBoundingBoxInQuery, false);

            var clearBoundingBoxElement = document.createElement('div');
            clearBoundingBoxElement.className = 'clearBoundingBoxInQuery ol-unselectable ol-control';
            clearBoundingBoxElement.appendChild(clearBoundingBoxButton);

            var boundingBoxControlsElement = document.createElement('div');
            boundingBoxControlsElement.appendChild(boundingBoxElement);
            boundingBoxControlsElement.appendChild(clearBoundingBoxElement);
            var boundingBoxControl = new ol.control.Control({
                element: boundingBoxControlsElement
            });
            $scope.map.addControl(boundingBoxControl);

            // Setting Bounding Box in the query (button) - Ends


            // Button for clearing all selected pins
            var clearSelectedPinsIconElement = document.createElement('img');
            clearSelectedPinsIconElement.src = '../aqub/images/clearSelectedPins_150.svg';
            clearSelectedPinsIconElement.height = 18;
            clearSelectedPinsIconElement.width = 18;

            var clearSelectedPinsButton = document.createElement('button');
            clearSelectedPinsButton.id = 'clearSelectedPinsButtonId';
            clearSelectedPinsButton.title = "Clear all currently selected pins";
            clearSelectedPinsButton.disabled = 'true';

            clearSelectedPinsButton.appendChild(clearSelectedPinsIconElement);

            // Function called to make the passed pin as de-selected
            // Used when clicking on a selected pin
            function deselectPin(item) {

                var jsonItem = {};

                angular.forEach(item.getProperties(), function (property, key) {
                    if (key != 'geometry' && key != 'featureType' && key != 'east' && key != 'west' && key != 'north' && key != 'south')
                        jsonItem[key] = property
                });

                var containedObject = containedInListBasedOnURI(jsonItem, rowModel.selectedRelatedInstanceList, 'uri');
                if (containedObject.contained)
                    rowModel.selectedRelatedInstanceList.splice(containedObject.index, 1);

                // Show related entity results panel on the respective rowModel
                if (rowModel.shownEntitySearchResults == false && rowModel.selectedRelatedInstanceList.length > 0)
                    rowModel.shownEntitySearchResults = true;
                else if (rowModel.shownEntitySearchResults == true && rowModel.selectedRelatedInstanceList.length < 1)
                    rowModel.shownEntitySearchResults = false;

                // Respectively handle
                // rowModel.allRelatedSearchResultsIsSelected
                rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
                $scope.handleSelectAllRelatedSearchResults(rowModel);

            }

            // Clearing all the selected pins
            var clearSelectedPinsInQuery = function () {

                // De-selecting the respective pins (items) from the list of
                // instances
                for (var i = 0; i < $scope.select.getFeatures().getArray().length; i++) {
                    deselectPin($scope.select.getFeatures().getArray()[i]);
                }
                // Making Pins unselected
                $scope.select.getFeatures().clear();

                // Disabling button that clears selected pins
                document.getElementById("clearSelectedPinsButtonId").disabled = true;

                // Display message informing user that bounding box has been
                // removed
                $mdToast.show(
                        $mdToast.simple()
                        .textContent('All the pins have been deselected!')
                        .position('top right')
                        .parent(angular.element('#mapDialogMainContent'))
                        .hideDelay(3000)
                        );

                // Disabling button that clears selected pins if no pin is
                // selected
                if ($scope.select.getFeatures().getLength() <= 0)
                    $scope.pinsAreSelected = false;
                else
                    $scope.pinsAreSelected = true;
                document.getElementById("clearSelectedPinsButtonId").disabled = !$scope.pinsAreSelected;
                $scope.$apply();

            };

            clearSelectedPinsButton.addEventListener('click', clearSelectedPinsInQuery, false);

            var clearSelectedPinsElement = document.createElement('div');
            clearSelectedPinsElement.className = 'clearSelectedPinsInQuery ol-unselectable ol-control';
            clearSelectedPinsElement.appendChild(clearSelectedPinsButton);

            var clearSelectedPinsControlsElement = document.createElement('div');
            clearSelectedPinsControlsElement.appendChild(clearSelectedPinsElement);
            var clearSelectedPinsControl = new ol.control.Control({
                element: clearSelectedPinsControlsElement
            });
            $scope.map.addControl(clearSelectedPinsControl);

            // If the "alwaysShowPinsForSelectedInstances" configuration option
            // is enabled and
            // there is at least one selected instance, then enable the
            // "clearSelectedPinsButton" button

            // konna change
            // if
            // ($scope.configuration.relatedEntity.map.alwaysShowPinsForSelectedInstances
            // && rowModel.selectedRelatedInstanceList.length > 0)
            // document.getElementById("clearSelectedPinsButtonId").disabled =
            // false;


            // Adding "Searching by Toponyms" on the map

            // Code regarding toponyms - Start

            // Flying animation
            // location: coordinates
            // Example: https://openlayers.org/en/latest/examples/animation.html
            function flyTo(location, done) {

                var duration = 3000;
                var zoom = 11;// $scope.map.getView().getZoom();
                var parts = 2;
                var called = false;

                function callback(complete) {
                    --parts;
                    if (called) {
                        return;
                    }
                    if (parts === 0 || !complete) {
                        called = true;
                        done(complete);
                    }
                }

                $scope.map.getView().animate({
                    center: location,
                    duration: duration
                }, callback);
                $scope.map.getView().animate({
                    zoom: zoom - 3,
                    duration: duration / 2
                }, {
                    zoom: zoom,
                    duration: duration / 2
                }, callback);
            }

            // Instantiate with some options and add the Control
            var geocoder = new Geocoder('nominatim', {
                provider: 'osm',
                lang: 'en',
                placeholder: 'Search for ...',
                limit: 5,
                debug: false,
                autoComplete: true,
                keepOpen: false,
                preventDefault: true
            });

            $scope.map.addControl(geocoder);

            geocoder.on('addresschosen', function (evt) {
                console.log('get address');
                flyTo(evt.coordinate, function () { });
                /*
                 * $scope.map.setView( new ol.View({ center: evt.coordinate, //
                 * Default is [0, 0] zoom: 11 //Default is 2 }) );
                 */
            });
            // Code regarding toponyms - End

            // a DragBox interaction used to select features by drawing boxes
            $scope.dragBox = new ol.interaction.DragBox({
                condition: ol.events.condition.platformModifierKeyOnly
            });

            $scope.map.addInteraction($scope.dragBox);

            $scope.coordinates = [];
            $scope.coordinatesRegion = {};

            $scope.infoBox = document.getElementById('info');

            $scope.dragBox.on('boxend', function () {
                // Holding all 5 coordinates in a string and transform them to
                // the appropriate projection
                var coordinateStr = $scope.dragBox.getGeometry().transform('EPSG:3857', 'EPSG:4326').getCoordinates();
                // console.log('coordinateStr: ' + coordinateStr);
                $scope.convertCoordinatesToJson(coordinateStr);

                // Calling the service to retrieve the pins
                $scope.retrieveGeoData(false, false, null); 	// First boolean
                // stands for
                // ignoring
                // "maxResoultCountForShowingPinsOnInit"
                // (when drawing
                // rectangle)
                // and boundingBoxAction is a boolean denoting that this action
                // will be applied due to
                // bounding box drawing action
                // Second boolean stands for drawingBox action (if we are
                // drawing a bounding box)

                // required for immediate update
                // $scope.$apply();
            });

            // Coordinates are shown as (latitude, longitude) pairs and not the
            // opposite
            // which is the usual way of presenting them.
            //
            // Coordinates are delivered circular like shown below. The first
            // pair is the
            // same as the last (fifth) pair due to polygon and not rectangular.
            //	
            // [[[0,1],[2,3],[4,5],[6,7],[8,9]]]
            //
            // (8,9)
            // (0,1) (6,7)
            // ---------
            // | |
            // | |
            // ---------
            // (2,3) (4,5)
            //	

            // boundingBoxAction is a boolean denoting whether this is an
            // action to be applied when drawing bounding box
            $scope.convertCoordinatesToJson = function (coordinateStr) {

                var thebox = coordinateStr.toString().split(",");
                // Using parseFloat in order to convert the strings to floats
                // and been able to apply comparisons
                var latitude1 = parseFloat(thebox[0]);
                var longitude1 = parseFloat(thebox[1]);
                var latitude2 = parseFloat(thebox[2]);
                var longitude2 = parseFloat(thebox[3]);
                var latitude3 = parseFloat(thebox[4]);
                var longitude3 = parseFloat(thebox[5]);
                var latitude4 = parseFloat(thebox[6]);
                var longitude4 = parseFloat(thebox[7]);
                var latitude5 = parseFloat(thebox[8]);	// This is the same as
                // latitude1
                var longitude5 = parseFloat(thebox[9]); // This is the same as
                // longitude1

                $scope.coordinates = [
                    {longitude1: longitude1, latitude1: latitude1},
                    {longitude2: longitude2, latitude2: latitude2},
                    {longitude3: longitude3, latitude3: latitude3},
                    {longitude4: longitude4, latitude4: latitude4},
                    {longitude5: longitude5, latitude5: latitude5},
                ];

                // Because there are many ways (4 different ways) of drawing the
                // rectangle
                // (i.e. top-left to bottom-right or bottom-left to top-right),
                // we have to
                // determine nort-south and west-east according to which one is
                // greatest or smallest

                // Determining north and south
                var north;
                var south;

                if (longitude1 != longitude2) {
                    if (longitude1 > longitude2) {
                        north = longitude1;
                        south = longitude2;
                    } else {
                        north = longitude2;
                        south = longitude1;
                    }
                } else { // if(longitude1 == longitude2)
                    if (longitude1 > longitude3) {
                        north = longitude1;
                        south = longitude3;
                    } else {
                        north = longitude3;
                        south = longitude1;
                    }
                }

                // Determining west and east
                var west;
                var east;

                if (latitude1 != latitude4) {
                    if (latitude1 < latitude4) {
                        west = latitude1;
                        east = latitude4;
                    } else {
                        west = latitude4;
                        east = latitude1;
                    }
                } else { // if(latitude1 == latitude4) {
                    if (latitude1 < latitude2) {
                        west = latitude1;
                        east = latitude2;
                    } else {
                        west = latitude2;
                        east = latitude1;
                    }
                }

                // $scope.coordinatesRegion = {north: latitude1, south:
                // latitude2, west: longitude1, east: longitude4}
                $scope.coordinatesRegion = {north: north, south: south, west: west, east: east}
                console.log(longitude1 + ", " + latitude1 + ", " + longitude2 + ", " + latitude2 + ", " + longitude3 + ", " + latitude3 + ", " + longitude4 + ", " + latitude4 + ", " + longitude5 + ", " + latitude5);
                console.log("north: " + north + ", south: " + south + ", west: " + west + ", east: " + east);

            }

            // Initializing
            $scope.polyFeatures = new ol.Collection();	// Array to hold the
            // polygons
            $scope.pointFeatures = new ol.Collection();	// Array to hold the
            // points

            var polyVectorLayer = new ol.layer.Vector();
            var pointVectorLayer = new ol.layer.Vector();

            $scope.select = new ol.interaction.Select();
            var popoverElement = document.getElementById('popup');
            var element = null; // The Popup Element

            // clear selection when drawing a new box and when clicking on the
            // map
            $scope.dragBox.on('boxstart', function () {
                // $scope.infoBox.innerHTML = '&nbsp;';
                $scope.polyFeatures.clear();
                $scope.pointFeatures.clear();
                $scope.select.getFeatures().clear();
            });

            // Styling Pins

            // Unselected Pink Icon
            var iconStylePinkUnselected = new ol.style.Style({
                image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
                    anchor: [0.3, 1],
                    offset: [22, 0],
                    size: [128, 128],
                    src: '../aqub/images/Map-Marker-Marker-Outside-Pink-icon.png',
                    // src: '../images/map-pin.svg',
                    // color: '#8959A8',
                    scale: 0.3
                }))
            });

            // Selected Pink Icon
            var iconStylePinkSelected = new ol.style.Style({
                image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
                    anchor: [0.3, 1],
                    offset: [22, 0],
                    size: [128, 128],
                    src: '../aqub/images/Map-Marker-Marker-Inside-Pink-icon.png',
                    scale: 0.3
                }))
            });

            // Unselected Green Icon
            var iconStyleGreenUnselected = new ol.style.Style({
                image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
                    anchor: [0.3, 1],
                    offset: [22, 0],
                    size: [128, 128],
                    src: '../aqub/images/Map-Marker-Marker-Inside-Chartreuse-icon.png',
                    scale: 0.3
                }))
            });

            // Selected Green Icon
            var iconStyleGreenSelected = new ol.style.Style({
                image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
                    anchor: [0.3, 1],
                    offset: [22, 0],
                    size: [128, 128],
                    src: '../images/Map-Marker-Marker-Inside-Chartreuse-icon.png',
                    scale: 0.3
                }))
            });

            // Unselected Blue Icon
            var iconStyleBlueUnselected = new ol.style.Style({
                image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
                    anchor: [0.3, 1],
                    offset: [22, 0],
                    size: [128, 128],
                    src: '../aqub/images/Map-Marker-Marker-Outside-Azure-icon.png',
                    scale: 0.3
                }))
            });

            // Selected Blue Icon
            var iconStyleBlueSelected = new ol.style.Style({
                image: new ol.style.Icon(/** @type {olx.style.IconOptions} */({
                    anchor: [0.3, 1],
                    offset: [22, 0],
                    size: [128, 128],
                    src: '../aqub/images/Map-Marker-Marker-Inside-Azure-icon.png',
                    scale: 0.3
                }))
            });

            // Pop-up preparation
            var popup = new ol.Overlay({
                element: popoverElement,
                positioning: 'bottom-center',
                stopEvent: true, // If false popover's click and wheel events
                // won't work
                offset: [0, -50]
            });

            // Displaying on map
            function handleGeoResultsForMap(geoResults, selectThemAll) {

                if ($scope.map.getLayers().getLength() > 1) {

                    // Removing with inverse order all layers apart from that
                    // one with index 0 and 1
                    // (0 is for the map and 1 is for the drawn bounding box)
                    for (i = $scope.map.getLayers().getLength(); i > 1; i--) {
                        $scope.map.removeLayer($scope.map.getLayers().item(i));
                    }
                }

                // Style for the polygons to be used for presenting the region

                var areaStyle = new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: 'blue',
                        width: 1
                    }),
                    fill: new ol.style.Fill({
                        color: 'rgba(0, 0, 255, 0.1)'
                    })
                });

                // A vector layer holding the polygon features
                polyVectorLayer = new ol.layer.Vector({
                    name: 'polyVectorLayer',
                    source: new ol.source.Vector({
                        features: $scope.polyFeatures
                    })
                });

                $scope.map.addLayer(polyVectorLayer);	// Adding Layer with all
                // the polygons

                // A vector layer holding the point features
                pointVectorLayer = new ol.layer.Vector({
                    name: 'pointVectorLayer',
                    source: new ol.source.Vector({
                        features: $scope.pointFeatures
                    }),
                    style: iconStylePinkUnselected
                });


                // Using Clustering ###########################################
                // - Starts
                // Works however there will be conflict with selecting instantly
                // few pins in region
                // and needs work to retain previous functionality

                // I also have to put in comment the:
                // 1. uncomment from index.html the lines:
                // <link rel="stylesheet"
                // href="https://cdn.rawgit.com/Viglino/ol-ext/master/dist/ol-ext.min.css"
                // />
                // <script type="text/javascript"
                // src="https://cdn.rawgit.com/Viglino/ol-ext/master/dist/ol-ext.min.js"></script>
                // 2. pointVectorLayer = new ol.layer.Vector({ ... })
                // 3. $scope.map.addLayer(pointVectorLayer);
                /*
                 * var getStyle = function(feature) {
                 * 
                 * var length = feature.get('features').length; return [ new
                 * ol.style.Style({
                 * 
                 * image: new ol.style.Circle({ radius: Math.min(
                 * Math.max(length * 0.8, 10), 15 ), fill: new ol.style.Fill({
                 * color: [0, 204, 0, 0.6] }) }), text: new ol.style.Text({
                 * text: length.toString(), fill: new ol.style.Fill({ color:
                 * 'black' }) }), stroke: new ol.style.Stroke({ color: [0, 51,
                 * 0, 1], width: 1 }), font: '26px "Helvetica Neue", Arial' }) ]; };
                 * 
                 * var clusterSource = new ol.source.Cluster({ distance: 100,
                 * source: new ol.source.Vector({ features: $scope.pointFeatures })
                 * }); // Animated cluster layer var clusterLayer = new
                 * ol.layer.AnimatedCluster({ source: clusterSource, // Use a
                 * style function for cluster symbolisation style: getStyle });
                 * 
                 * $scope.map.addLayer(clusterLayer);
                 */
                // Using Clustering ###########################################
                // - ENDS

                $scope.map.addLayer(pointVectorLayer);	// Adding Layer with all
                // the points

                // Adding pop-up
                $scope.map.addOverlay(popup);

                // The pop-up Element
                element = popup.getElement();

                // Adding hovering

                var hoverInteraction = new ol.interaction.Select({
                    condition: ol.events.condition.pointerMove,
                    layers: [pointVectorLayer], // Setting layers to be hovered
                    style: iconStyleGreenUnselected
                });
                $scope.map.addInteraction(hoverInteraction);

                // Adding select for pins
                $scope.select = new ol.interaction.Select({
                    layers: [pointVectorLayer],
                    style: iconStyleGreenSelected,
                    toggleCondition: ol.events.condition.always
                });
                $scope.map.addInteraction($scope.select);

                // Function called to make the passed item as selected
                function selectPin(item) {

                    var jsonItem = {};

                    // console.log('evt.selected: ');
                    angular.forEach(item.getProperties(), function (property, key) {
                        // console.log(key + ': ' + property.value);
                        // if(key != 'geometry' && key != 'featureType' && key
                        // != 'east' && key != 'west' && key != 'north' && key
                        // != 'south')
                        if (key != 'geometry' && key != 'featureType')
                            jsonItem[key] = property
                    });

                    // Adding into list of selected instances of the rowModel if
                    // not already there
                    if (!containedInListBasedOnURI(jsonItem, rowModel.selectedRelatedInstanceList, 'uri').contained)
                        rowModel.selectedRelatedInstanceList.push(jsonItem);

                    // Show related entity results panel on the respective
                    // rowModel
                    if (rowModel.shownEntitySearchResults == false && rowModel.selectedRelatedInstanceList.length > 0)
                        rowModel.shownEntitySearchResults = true;
                    else if (rowModel.shownEntitySearchResults == true && rowModel.selectedRelatedInstanceList.length < 1)
                        rowModel.shownEntitySearchResults = false;

                    // Respectively handle
                    // rowModel.allRelatedSearchResultsIsSelected
                    rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
                    $scope.handleSelectAllRelatedSearchResults(rowModel);

                }

                // On Select
                $scope.select.on('select', function (evt) {

                    // console.log('$scope.select: ' +
                    // $scope.select.getFeatures().getArray());

                    // Select
                    if (evt.selected.length > 0) {

                        // When the size limit regulation (by configuration) is
                        // not yet reached
                        if (rowModel.selectedRelatedInstanceList.length < $scope.configuration.relatedEntity.selectedInstancesLimit) {
                            selectPin(evt.selected[0]);

                            // Enabling button that clears selected pins
                            document.getElementById("clearSelectedPinsButtonId").disabled = false;

                        }

                        // When the size limit regulation (by configuration) is
                        // reached
                        else { // if
                            // (rowModel.selectedRelatedInstanceList.length
                            // >=
                            // $scope.configuration.relatedEntity.selectedInstancesLimit)

                            // Remove this feature from the list of selected
                            // ones
                            $scope.select.getFeatures().remove(evt.selected[0]);

                            $scope.message = 'You cannot have more than '
                                    + $scope.configuration.relatedEntity.selectedInstancesLimit
                                    + ' instances selected for this related entity';
                            $scope.showAlertOnMap('Information', $scope.message);
                        }

                    } else {

                    }

                    // Deselect
                    if (evt.deselected.length > 0) {
                        deselectPin(evt.deselected[0]);
                    }

                    // Disabling button that clears selected pins if no pin is
                    // selected
                    if ($scope.select.getFeatures().getLength() <= 0)
                        $scope.pinsAreSelected = false;
                    else
                        $scope.pinsAreSelected = true;
                    document.getElementById("clearSelectedPinsButtonId").disabled = !$scope.pinsAreSelected;
                    $scope.$apply();

                });

                // Handling Results on the map
                for (i = 0; i < geoResults.length; i++) {

                    // Polygon Feature (rectangular)
                    var polyFeature = new ol.Feature({
                        geometry: new ol.geom.Polygon([
                            [
                                [parseFloat(geoResults[i].west.value), parseFloat(geoResults[i].north.value)],
                                [parseFloat(geoResults[i].west.value), parseFloat(geoResults[i].south.value)],
                                [parseFloat(geoResults[i].east.value), parseFloat(geoResults[i].south.value)],
                                [parseFloat(geoResults[i].east.value), parseFloat(geoResults[i].north.value)],
                                [parseFloat(geoResults[i].west.value), parseFloat(geoResults[i].north.value)]
                            ]
                        ])
                    });


                    polyFeature.setStyle(areaStyle);
                    $scope.polyFeatures.push(polyFeature); // Adding into Array

                    // Constructing the pointFeature dynamically
                    var pointFeature = new ol.Feature();
                    pointFeature.setGeometry(polyFeature.getGeometry().getInteriorPoint());
                    pointFeature.set('featureType', 'marker');

                    // Leaving it as it is in the original data and will
                    // be changed when constructing the actual pop-up element
                    angular.forEach(geoResults[i], function (property, key) {
                        pointFeature.set(key, property);
                    });

                    // Change coordinate systems to display on the map
                    polyFeature.getGeometry().transform('EPSG:4326', 'EPSG:3857');
                    pointFeature.getGeometry().transform('EPSG:4326', 'EPSG:3857');


                    if (selectThemAll) {
                        selectPin(pointFeature);
                        $scope.select.getFeatures().push(pointFeature);
                    }

                    // Marking as checked if already selected
                    else if (containedInListBasedOnURI(geoResults[i], rowModel.selectedRelatedInstanceList, 'uri').contained)
                        $scope.select.getFeatures().push(pointFeature);

                    $scope.pointFeatures.push(pointFeature); // Adding into
                    // Array

                    // Setting unselected style
                    // pointFeature.setStyle(iconStylePinkUnselected);

                } // loop ends

                if (selectThemAll) {

                    // Regarding Bounding Box
                    $scope.boxSource.clear(); // Clearing drawn bounding box
                    if (rowModel.boundingBox != undefined) {
                        delete rowModel.boundingBox; // Removing bounding box
                        // from the model
                        $scope.fewPinsInsideBoundingBox = []; // Initializing
                        // array holding
                        // pins inside
                        // bounding box
                        // if they are
                        // few
                    }

                    // Disabling button that clears bounding box
                    document.getElementById("clearBoundingBoxButtonId").disabled = true;

                    // Display message informing user that bounding box has been
                    // set
                    /*
                     * $mdToast.show( $mdToast.simple() .textContent('The
                     * available instances of the entity \'' +
                     * rowModel.selectedRelatedEntity.name + '\' within the
                     * bounding box set are very few and thus automatically
                     * selected.') .position('top right')
                     * .parent(angular.element('#mapDialogMainContent'))
                     * .hideDelay(60000) .action('OK')
                     * //.theme("important-toast") );
                     */
                }

            }

            // On Hover

            // Used for changing cursor type
            var target = $scope.map.getTarget();
            var jTarget = typeof target === "string" ? $("#" + target) : $(target);

            // Used for not rendering it on every pixel when already displayed
            var isDisplayed = false;

            // Holding some history of the moves for reference
            var previousFeature = new ol.Feature(); // The previous feature
            // hovered over the map (can
            // be null)
            var oldFeature = new ol.Feature();		// The previous feature
            // hovered over any pin (can
            // never null)

            // On Hover
            $scope.map.on('pointermove', function (e) {

                // Changing mouse cursor when over pointFeature
                var pixel = $scope.map.getEventPixel(e.originalEvent);
                var hit = $scope.map.hasFeatureAtPixel(pixel);
                if (hit) {
                    jTarget.css("cursor", "pointer");
                } else {
                    jTarget.css("cursor", "");
                }

                // Only return marker Features (not polygons) or null
                var feature = $scope.map.forEachFeatureAtPixel(pixel, function (newFeature) {
                    if (newFeature.get('featureType') == 'marker') {
                        // Don't redisplay if the new feature
                        // is the same as the previous one
                        // (hovering over the pixels of the same pin)
                        if (previousFeature != null) {
                            if (previousFeature.get('uri') == newFeature.get('uri'))
                                isDisplayed = true;
                            else
                                isDisplayed = false;
                        }
                        // Don't redisplay if the new feature
                        // has the popup already open
                        // (hovering over some pin then go out and then
                        // hover immediately over the same pin)
                        else if (oldFeature != null) {
                            if (oldFeature.get('uri') == newFeature.get('uri'))
                                isDisplayed = true;
                            else
                                isDisplayed = false;
                        }
                        // Any other case
                        else
                            isDisplayed = false;

                        return newFeature;
                    } else {
                        isDisplayed = true;
                        return null;
                    }
                });

                // Holding a copy of the feature to compare it with the new one
                // in the future
                // and decide whether to display the popup or not. The actual
                // usage is in the
                // cases where the pins are stick together with out gap. If
                // there is gap, it
                // is detected and the isDisplayed flag becomes false, but if it
                // dosn't exist
                // I'm using the comparison of the old & new features.
                // previousFeature = $.extend( {}, feature);
                // previousFeature = angular.copy(feature);
                previousFeature = feature;

                if (feature != null) { // Thus it is a marker

                    oldFeature = feature;

                    if (!isDisplayed) {

                        var coordinates = feature.getGeometry().getCoordinates();
                        popup.setPosition(coordinates);

                        // This is how to get keys or properties
                        // Keys is an array of strings
                        // properties is a JSON element {key1: value1, key2:
                        // value2 ...}
                        // console.log("feature.getKeys():");
                        // console.log(feature.getKeys());
                        // console.log("feature.getProperties():");
                        // console.log(feature.getProperties());

                        // Dynamically constructing the element from the
                        // featurePoint
                        if ($(element) != null) {
                            $(element).attr('data-placement', 'top');
                            $(element).attr('data-original-title', '<b>' + 'Info' + '</b>');
                            $(element).attr('data-animation', true);
                            $(element).attr('data-html', true);

                            var htmlContent = '';
                            var propIndex = 0; // geometry and featureType are
                            // the two first ones
                            angular.forEach(feature.getProperties(), function (property, key) {
                                if (key != 'geometry' && key != 'featureType' && key != 'east' &&
                                        key != 'west' && key != 'north' && key != 'south') {
                                    if (property.type != 'uri') {
                                        if (propIndex == 2)
                                            htmlContent = htmlContent + '<a href=\"' + feature.getProperties().uri.value + '\" target="_blank">' + property.value + '</a>' + '<br/><br/>';
                                        else
                                            htmlContent = htmlContent + '<span style=\"text-decoration: underline;\">' + key + ':</span> <i>' + property.value + '</i><br/>';
                                    }
                                }
                                $(element).attr('data-content', htmlContent);
                                propIndex++;
                            });

                            // $(element).attr('data-content',
                            // feature.get('name') + " by " +
                            // feature.get('Service') + "</br></br><span
                            // style=\"text-decoration:
                            // underline;\">Responsible:</span> <i>" +
                            // feature.get('Responsible') + "</i>");
                            $(element).popover();
                            $(element).popover('show');
                        }
                    }
                } else {
                    // $(element).popover('destroy');
                }

            });

            // Clicking anywhere on the map
            // Tthis is different than selecting a feature (used above)
            $scope.map.on('click', function (evt) {
                console.log(evt);

                // Determine whether anything but a pin is clicked

                var feature = $scope.map.forEachFeatureAtPixel(evt.pixel, function (feature) {
                    return feature;
                });

                // When a rectangle is clicked then destroy popup
                if (feature != undefined) {
                    if (feature.get('featureType') != 'marker') {
                        if ($(element) != null)
                            $(element).popover('destroy');
                    }
                }
                // When the map (not any pin) is clicked then destroy popup
                else {
                    if ($(element) != null)
                        $(element).popover('destroy');
                }

            });

            // considerTotalResultCount: Boolean used to decide whether to
            // consider "maxResoultCountForShowingPinsOnInit"
            // or not. Basically we use false to display pins when drawing
            // rectangle and true to display pins when opening
            // the dialog if their count is less than
            // "maxResoultCountForShowingPinsOnInit"
            // boundingBoxAction: Boolean denoting whether this is an action to
            // be applied when drawing bounding box
            $scope.retrieveGeoData = function (considerTotalResultCount, boundingBoxAction, ev) {

                // Modal
                var modalOptions = {
                    headerText: 'Loading Please Wait...',
                    bodyText: 'Search process undergoing...'
                };

                if (considerTotalResultCount) { // Options when first opening
                    // this dialog
                    modalOptions = {
                        headerText: 'Loading Please Wait...',
                        bodyText: 'Loading options...'
                    };
                }

                var modalInstance = modalService.showModal(modalDefaults, modalOptions);

                // Some dynamically defined preparation

                // The search text to feed the query
                var querySearchText = '';

                angular.forEach(rowModel.relatedChips, function (value, key) {
                    querySearchText = querySearchText + ' ' + value.name;
                });

                if (rowModel.relatedEntitySearchText != null && rowModel.relatedEntitySearchText != '') {
                    querySearchText = querySearchText + ' ' + rowModel.relatedEntitySearchText;
                }

                // var model = rowModel.selectedTargetEntity !== undefined ?
                // rowModel.selectedTargetEntity :rowModel.selectedRelatedEntity;
                var geospathial = rowModel.selectedRelatedEntity.geo_query == "" ? false : true;
                var searchEntityModel = {
                    entity: rowModel.selectedRelatedEntity.name,
                    geo_query: rowModel.selectedRelatedEntity.geo_query,
                    // konsolak comment temp
                    // text_geo_query:
                    // rowModel.selectedRelatedEntity.text_geo_query,
                    geospatial: rowModel.selectedRelatedEntity.geospatial,
                    searchText: querySearchText,
                    relatedChips: rowModel.relatedChips,
                    fromSearch: $scope.queryFrom,
                    north: $scope.coordinatesRegion.north,
                    south: $scope.coordinatesRegion.south,
                    west: $scope.coordinatesRegion.west,
                    east: $scope.coordinatesRegion.east
                }

                var updatedQueryModel = '';

                queryService.computeRelatedEntityQuery(searchEntityModel, $scope.credentials.token).then(function (queryResponse) {
                    if (queryResponse.status == '200') {
                        console.log('queryResponse:');
                        console.log(queryResponse);
                        console.log(rowModel.selectedRelatedEntity);
                        updatedQueryModel = angular.copy(rowModel.selectedRelatedEntity)
                        updatedQueryModel.query = queryResponse.data.query;

                        delete updatedQueryModel.geo_query;
                        delete updatedQueryModel.text_geo_query;
                        console.log('Geospatial Query:');
                        console.log(updatedQueryModel);

                        // Only for case where results should be loaded when
                        // opening the map
                        //
                        // The hack is that we add a limit 1 more to the allowed
                        // one and then we should check if the
                        // total results are more than the allowed (if they are
                        // more, they will be just one more)
                        // Then we either use the results or just throw them
                        // away
                        if (considerTotalResultCount) { // Here we ignore this
                            // future for displaying
                            // pins inside the
                            // pre-defined region on
                            // load of the map

                            // Limit for showing pins on map without bounding
                            // box area
                            if (rowModel.boundingBox == undefined)
                                updatedQueryModel.query = updatedQueryModel.query + ' limit ' + ($scope.configuration.relatedEntity.map.maxResoultCountForShowingPinsOnInit + 1);
                            // Limit for showing pins on map with bounding box
                            // area
                            // else
                            // updatedQueryModel.query = updatedQueryModel.query
                            // + ' limit ' +
                            // ($scope.configuration.relatedEntity.map.maxNumOfPinsInBoundingBoxOnInit
                            // + 1);

                        }

                        // Calling service to executing Query - Promise
                        var data = {
                            "query": updatedQueryModel.query,
                            "format": "application/json"
                        }

                        queryService.getEntityQueryResults($scope.serviceModel, data, $scope.credentials.token)
                                .then(function (response) {
                                    console.log('RESPONSE:' + response);
                                    if (response.status == -1) {
                                        $scope.message = 'There was a network error. Try again later.';
                                        $scope.showErrorAlert('Error', $scope.message);
                                        modalInstance.close();
                                    } else {
                                        // Checking the response from blazegraph
                                        if (response.status == '200') {
                                            // handling results

                                            // Adding existing instances on the
                                            // results such that they are loaded as
                                            // well
                                            // (configurable:
                                            // alwaysShowPinsForSelectedInstances)
                                            if ($scope.configuration.relatedEntity.map.alwaysShowPinsForSelectedInstances) {
                                                rowModel.selectedRelatedInstanceList
                                                response.data.results.bindings

                                                // Array to hold those already
                                                // selected instances that
                                                // do not exist in results (avoiding
                                                // duplicated pins)
                                                var currentInstancesNotInResults = [];

                                                for (i = 0; i < rowModel.selectedRelatedInstanceList.length; i++) {
                                                    if (!containedInListBasedOnURI(rowModel.selectedRelatedInstanceList[i], response.data.results.bindings, 'uri').contained)
                                                        currentInstancesNotInResults.push(rowModel.selectedRelatedInstanceList[i]);
                                                }

                                                // Adding the not contained
                                                // instances from the selected ones
                                                // to the results
                                                for (i = 0; i < currentInstancesNotInResults.length; i++) {
                                                    response.data.results.bindings.push(currentInstancesNotInResults[i]);
                                                }

                                            }

                                            // Case where results should be loaded
                                            // when opening the map
                                            if (considerTotalResultCount) {

                                                if (rowModel.boundingBox == undefined) {

                                                    // Checking if count is less or
                                                    // equal to the allowed limit
                                                    if (response.data.results.bindings.length < $scope.configuration.relatedEntity.map.maxResoultCountForShowingPinsOnInit) {
                                                        handleGeoResultsForMap(response.data.results.bindings, false); // false
                                                        // stands
                                                        // for
                                                        // not
                                                        // selecting
                                                        // them
                                                        // all

                                                        // Only reset if there is
                                                        // not any bounding box
                                                        // already set
                                                        if (rowModel.boundingBox == undefined) {
                                                            // Reset zoom level and
                                                            // center to default
                                                            $scope.map.getView().setCenter([0, 0]);
                                                            $scope.map.getView().setZoom(2);
                                                        }
                                                    }

                                                    // Just show the selected
                                                    // instances
                                                    else if ($scope.configuration.relatedEntity.map.alwaysShowPinsForSelectedInstances) {
                                                        handleGeoResultsForMap(rowModel.selectedRelatedInstanceList, false); // false
                                                        // stands
                                                        // for
                                                        // not
                                                        // selecting
                                                        // them
                                                        // all
                                                    }

                                                }

                                                // Pre-drawing the bounding box if
                                                // exist

                                                // If the model has bounding box,
                                                // then display it on the map
                                                else {// (rowModel.boundingBox !=
                                                    // undefined) {

                                                    boundingBoxFeatures.push($scope.currBoundingBoxFeature);

                                                    // If
                                                    // showPinsWhenDrawingBoundingBox
                                                    // is set in the configuration
                                                    // setup
                                                    if ($scope.configuration.relatedEntity.map.showPinsWhenDrawingBoundingBox) {

                                                        // Checking if count is less
                                                        // or equal to the allowed
                                                        // limit (specific allowed
                                                        // number of pins to show
                                                        // within bounding box when
                                                        // opening map)
                                                        if (response.data.results.bindings.length < $scope.configuration.relatedEntity.map.maxNumOfPinsInBoundingBoxOnInit)
                                                            handleGeoResultsForMap(response.data.results.bindings, false); // false
                                                        // stands
                                                        // for
                                                        // not
                                                        // selecting
                                                        // them
                                                        // all
                                                        // Ask whether to show them
                                                        // or not
                                                        else {

                                                            var messageContent = 'There are a lot of '
                                                                    + '<code style="color:#106CC8;background: rgba(0,0,0,0.065);">'
                                                                    + rowModel.selectedRelatedEntity.name
                                                                    + '</code>'
                                                                    + ' instances to show within the region previously set.'
                                                                    + '<br/>'
                                                                    + 'This is an action that might take a while to complete, however you '
                                                                    + 'can skip that and just show the marked region instead.'
                                                                    + '<br/><br/>'
                                                                    + 'What do you want to do?';

                                                            var confirm = $mdDialog.confirm()
                                                                    .parent(angular.element(document.querySelector('#popupContainerOnMap')))
                                                                    .title('Important Message - Confirmation Required')
                                                                    .htmlContent(messageContent)
                                                                    .ariaLabel('Remove selected bounding box - Confirmation')
                                                                    .targetEvent(ev)
                                                                    .ok('Show instances within the marked region')
                                                                    .cancel('Just show the marked region (no pins)')
                                                                    .multiple(true);

                                                            $mdDialog.show(confirm).then(function () { // OK
                                                                handleGeoResultsForMap(response.data.results.bindings, false); // false
                                                                // stands
                                                                // for
                                                                // not
                                                                // selecting
                                                                // them
                                                                // all
                                                            }, function () { // Cancel
                                                                // Do nothing
                                                            });


                                                        }
                                                    }

                                                    // Re-center the map
                                                    $scope.map.getView().setCenter($scope.currBoundingBoxFeature.getGeometry().getInteriorPoint().getCoordinates());

                                                    // Re-Zooming the map
                                                    if (rowModel.map != undefined) {
                                                        if (rowModel.map.zoom != undefined)
                                                            $scope.map.getView().setZoom(rowModel.map.zoom);
                                                    }

                                                }

                                            }

                                            // Case where results are loaded by
                                            // drawing rectangle or bounding box
                                            else {// if(!considerTotalResultCount)
                                                // {

                                                // Case drawing Bounding box
                                                if (boundingBoxAction) {

                                                    $scope.fewPinsInsideBoundingBox = []; // Initializing
                                                    // array
                                                    // holding
                                                    // pins
                                                    // inside
                                                    // bounding
                                                    // box
                                                    // if
                                                    // they
                                                    // are
                                                    // few

                                                    // If there is at least one pin
                                                    // apply the following,
                                                    // otherwise don't do anything
                                                    if (response.data.results.bindings.length > 0) {

                                                        // Holding their count into
                                                        // the rowModel
                                                        rowModel.boundingBoxResultsCount = response.data.results.bindings.length;

                                                        // Show the pins if
                                                        // configured, otherwise
                                                        // don't (the drawn bounding
                                                        // box stays empty)
                                                        if ($scope.configuration.relatedEntity.map.showPinsWhenDrawingBoundingBox)
                                                            handleGeoResultsForMap(response.data.results.bindings, false); // false
                                                        // stands
                                                        // for
                                                        // not
                                                        // selecting
                                                        // any
                                                        // of
                                                        // them

                                                        // Setting bounding box on
                                                        // rowModel
                                                        rowModel.boundingBox = $scope.coordinatesRegion;
                                                        // Setting map's current
                                                        // zoom on rowModel
                                                        rowModel.map = {zoom: $scope.map.getView().getZoom()};
                                                        // Display message informing
                                                        // user that bounding box
                                                        // has been set
                                                        $mdToast.show(
                                                                $mdToast.simple()
                                                                .textContent('Bounding box has been set!')
                                                                .position('top right')
                                                                .parent(angular.element('#mapDialogMainContent'))
                                                                .hideDelay(3000)
                                                                );

                                                        // Setting query under
                                                        // construction
                                                        homeStateConfirmService.setQueryUnderConstruction(true);
                                                        $scope.markFavoriteAsChanged(homeStateConfirmService.isQueryUnderConstruction()); // Mark
                                                        // favorite
                                                        // as
                                                        // changed
                                                        // (if
                                                        // favorite)

                                                        // If the number of pins is
                                                        // few, then hold them in
                                                        // scope
                                                        if (response.data.results.bindings.length <= $scope.configuration.relatedEntity.map.minResoultCountForAutoSelectingPinsOnDrawingBox) {
                                                            $scope.fewPinsInsideBoundingBox = response.data.results.bindings;
                                                        }
                                                    } else {
                                                        rowModel.boundingBoxResultsCount = 0;
                                                    }

                                                }

                                                // Case drawing rectangle
                                                else {
                                                    // Holding their count into the
                                                    // rowModel
                                                    rowModel.boundingBoxResultsCount = response.data.results.bindings.length;
                                                    handleGeoResultsForMap(response.data.results.bindings, false); // false
                                                    // stands
                                                    // for
                                                    // not
                                                    // selecting
                                                    // any
                                                    // of
                                                    // them
                                                }

                                                // Disabling button that clears
                                                // selected pins if no pin is
                                                // selected
                                                if ($scope.select.getFeatures().getLength() <= 0)
                                                    $scope.pinsAreSelected = false;
                                                else
                                                    $scope.pinsAreSelected = true;
                                                document.getElementById("clearSelectedPinsButtonId").disabled = !$scope.pinsAreSelected;

                                            }

                                            modalInstance.close();

                                        } else if (response.status == '408') {
                                            $log.info(response.status);
                                            $scope.message = 'It seems that it takes a lot of time to complete this task! Please redifine your query and try again.';
                                            $scope.showErrorAlert('Important', $scope.message);
                                        } else if (response.status == '400') {
                                            $log.info(response.status);
                                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                            $scope.showErrorAlert('Error', $scope.message);
                                            modalInstance.close();
                                        } else if (response.status == '401') {
                                            $log.info(response.status);
                                            modalInstance.close();
                                            $scope.showLogoutAlert();
                                            authenticationService.clearCredentials();
                                        } else {
                                            $log.info(response.status);
                                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                                            $scope.showErrorAlert('Error', $scope.message);
                                            modalInstance.close();
                                        }

                                    } // else close

                                }, function (error) {
                                    $scope.message = 'There was a network error. Try again later.';
                                    alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                                        data: error
                                    }));
                                    modalInstance.close();
                                });
                        // Execute query promise - End
                    } else if (queryResponse.status == '400') {
                        $log.info(queryResponse.status);
                        $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                        $scope.showErrorAlert('Error', $scope.message);
                        modalInstance.close();
                    } else if (queryResponse.status == '401') {
                        $log.info(queryResponse.status);
                        modalInstance.close();
                        $scope.showLogoutAlert();
                        authenticationService.clearCredentials();
                    } else {
                        $log.info(queryResponse.status);
                        $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                        $scope.showErrorAlert('Error', $scope.message);
                        modalInstance.close();
                    }
                },
                        function (error) {
                            $scope.message = 'There was a network error. Try again later.';
                            alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                                data: error
                            }));
                            modalInstance.close();
                        });

            }



        }

        $scope.closeSelectedFromMapDialogDialog = function (ev, rowModel) {

            // Retrieve from backup
            rowModel.selectedRelatedInstanceList = angular.copy(rowModel.backupSelectedRelatedInstanceList);
            rowModel.boundingBox = angular.copy(rowModel.backupBoundingBox); // Keep
            // backup
            // of
            // bounding
            // box

            // Handle whether to show or hide related entity results panel on
            // the respective rowModel
            if (rowModel.selectedRelatedInstanceList.length > 0)
                rowModel.shownEntitySearchResults = true;
            else
                rowModel.shownEntitySearchResults = false;

            // Respectively handle rowModel.allRelatedSearchResultsIsSelected
            rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
            $scope.handleSelectAllRelatedSearchResults(rowModel);

            // Check if the list of instances is changed and call service to
            // reload option lists
            if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');
            $mdDialog.cancel();
        }

        // Triggered when pressing the respective button on the map
        // It actually removes the selected pins and keeps the region set
        // (bounding box)
        $scope.setRegionInQueryAndcloseMapDialogDialog = function (ev, rowModel) {

            var messageContent = 'There are certain instances already selected for this related entity. '
                    + '<br/>'
                    + 'By setting a geographical bounding box filter, these instances will be removed. '
                    + '<br/><br/>'
                    + 'Are you sure you want to continue with this action?';

            // If there are instances selected, ask permission to delete them
            // (bounding box should not co-exist along with selected instances)
            if (rowModel.selectedRelatedInstanceList.length > 0) {
                var confirm = $mdDialog.confirm()
                        .parent(angular.element(document.querySelector('#popupContainerOnMap')))
                        .title('Important Message - Confirmation Required')
                        .htmlContent(messageContent)
                        .ariaLabel('Remove previously selected instances - Confirmation')
                        .targetEvent(ev)
                        .ok('Yes Continue')
                        .cancel('Cancel')
                        .multiple(true);

                $mdDialog.show(confirm).then(function () { // OK

                    // Hide related entity results panel on the respective
                    // rowModel
                    rowModel.shownEntitySearchResults = false;
                    // Respectively handle
                    // rowModel.allRelatedSearchResultsIsSelected
                    rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
                    $scope.handleSelectAllRelatedSearchResults(rowModel);

                    // Dealing with the map
                    $scope.polyFeatures.clear();
                    $scope.pointFeatures.clear();
                    $scope.select.getFeatures().clear();

                    // Check if the list of instances is changed and call
                    // service to reload option lists
                    if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                        $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

                    $mdDialog.cancel();

                }, function () { // Cancel
                    // Do nothing
                });
            } else {
                // Setting flag for search by text
                rowModel.allRelatedSearchResultsIsSelected = true;
                // Removing all currently selected instances for the related
                // entity of this rowModel
                // rowModel.selectedRelatedInstanceList = [];
                // Hide related entity results panel on the respective rowModel
                rowModel.shownEntitySearchResults = false;

                // Respectively handle
                // rowModel.allRelatedSearchResultsIsSelected
                rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
                $scope.handleSelectAllRelatedSearchResults(rowModel);

                // Dealing with the map
                $scope.polyFeatures.clear();
                $scope.pointFeatures.clear();
                $scope.select.getFeatures().clear();

                // Check if the list of instances is changed and call service to
                // reload option lists
                if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                    $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

                $mdDialog.cancel();

            }

        }

        // Triggered when pressing the respective button on the map
        // It actually removes the bounding box set and keeps the selected pins
        $scope.setSelectedPinsInQueryAndcloseMapDialogDialog = function (ev, rowModel) {

            var messageContent = 'There is certain region marked on the map. '
                    + '<br/>'
                    + 'By Choosing to add the selected pins in the query, that region will be cleared. '
                    + '<br/><br/>'
                    + 'Are you sure you want to continue with this action?';

            // If there region marked, ask permission to delete it
            // (bounding box should not co-exist along with selected instances)
            if (rowModel.boundingBox != undefined) {
                var confirm = $mdDialog.confirm()
                        .parent(angular.element(document.querySelector('#popupContainerOnMap')))
                        .title('Important Message - Confirmation Required')
                        .htmlContent(messageContent)
                        .ariaLabel('Remove selected bounding box - Confirmation')
                        .targetEvent(ev)
                        .ok('Yes Continue')
                        .cancel('Cancel')
                        .multiple(true);

                $mdDialog.show(confirm).then(function () { // OK

                    // Removing flag for search by text
                    rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
                    // Respectively handle
                    // rowModel.allRelatedSearchResultsIsSelected
                    $scope.handleSelectAllRelatedSearchResults(rowModel);

                    // Clearing bounding box
                    $scope.boxSource.clear(); // Clearing drawn bounding box
                    rowModel.backupBoundingBox = angular.copy(rowModel.boundingBox); // Keep
                    // backup
                    // of
                    // bounding
                    // box
                    delete rowModel.boundingBox; // Removing bounding box
                    // from the model
                    $scope.fewPinsInsideBoundingBox = []; // Initializing
                    // array holding
                    // pins inside
                    // bounding box if
                    // they are few

                    // Check if the list of instances is changed and call
                    // service to reload option lists
                    if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                        $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

                    $mdDialog.cancel();

                }, function () { // Cancel
                    // Do nothing
                });
            } else {
                // Removing flag for search by text
                rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
                // Respectively handle
                // rowModel.allRelatedSearchResultsIsSelected
                $scope.handleSelectAllRelatedSearchResults(rowModel);

                // Clearing bounding box
                $scope.boxSource.clear(); // Clearing drawn bounding box
                rowModel.backupBoundingBox = angular.copy(rowModel.boundingBox); // Keep
                // backup
                // of
                // bounding
                // box
                delete rowModel.boundingBox; // Removing bounding box from
                // the model
                $scope.fewPinsInsideBoundingBox = []; // Initializing array
                // holding pins inside
                // bounding box if they
                // are few

                // Check if the list of instances is changed and call service to
                // reload option lists
                if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                    $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

                $mdDialog.cancel();

            }

        }

        // Triggered when pressing the respective button on the map
        // It actually removes the bounding box set and keeps the selected pins
        $scope.setPinsInsideBoundingBoxInQueryAndcloseMapDialogDialog = function (ev, rowModel) {
            flo

            // Add them all in the list of selected instances of the rowModel
            // (no duplicates)
            for (i = 0; i < $scope.fewPinsInsideBoundingBox.length; i++) {
                // Adding into list of selected instances of the rowModel if not
                // already there
                if (!containedInListBasedOnURI($scope.fewPinsInsideBoundingBox[i], rowModel.selectedRelatedInstanceList, 'uri').contained) {
                    // Delete a few properties before inserting it
                    delete $scope.fewPinsInsideBoundingBox[i].geometry;
                    delete $scope.fewPinsInsideBoundingBox[i].featureType;
                    // delete $scope.fewPinsInsideBoundingBox[i].east;
                    // delete $scope.fewPinsInsideBoundingBox[i].west;
                    // delete $scope.fewPinsInsideBoundingBox[i].north;
                    // delete $scope.fewPinsInsideBoundingBox[i].south;
                    rowModel.selectedRelatedInstanceList.push($scope.fewPinsInsideBoundingBox[i]);
                }
            }

            // Removing flag for search by text
            rowModel.shownEntitySearchResults = true;
            rowModel.allRelatedSearchResultsIsSelected = !angular.copy(rowModel.shownEntitySearchResults);
            // Respectively handle rowModel.allRelatedSearchResultsIsSelected
            $scope.handleSelectAllRelatedSearchResults(rowModel); // This
            // clears
            // the
            // fewPinsInsideBoundingBox
            // Array as
            // well

            // Clearing bounding box
            $scope.boxSource.clear(); // Clearing drawn bounding box
            rowModel.backupBoundingBox = angular.copy(rowModel.boundingBox); // Keep
            // backup
            // of
            // bounding
            // box
            delete rowModel.boundingBox; // Removing bounding box from the
            // model

            // Check if the list of instances is changed and call service to
            // reload option lists
            if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

            $mdDialog.cancel();

        }

        // Reseting map and then closing it.
        // By resetting is mainly meant clearing the bounding box and
        // de-selecting
        // the selected pins from those currently displayed (Any instances in
        // the
        // list of related instances that are not displayed and shown currently
        // on
        // the map will be retained)
        $scope.applyAndcloseMapDialogDialog = function (ev, rowModel) {

            var messageContent = 'Please note that any changes applied on the map will be finalized. '
                    + '<br/><br/>'
                    + 'Are you sure you want to continue with this action?';

            // If there region marked, ask permission to delete it
            // (bounding box should not co-exist along with selected instances)
            var confirm = $mdDialog.confirm()
                    .parent(angular.element(document.querySelector('#popupContainerOnMap')))
                    .title('Important Message - Confirmation Required')
                    .htmlContent(messageContent)
                    .ariaLabel('Reset Map actions - Confirmation')
                    .targetEvent(ev)
                    .ok('Yes Continue')
                    .cancel('Cancel')
                    .multiple(true);

            $mdDialog.show(confirm).then(function () { // OK

                // Check if the list of instances is changed and call service to
                // reload option lists
                if (angular.toJson(rowModel.selectedRelatedInstanceList) != angular.toJson(rowModel.backupSelectedRelatedInstanceList))
                    $scope.loadRelatedEntitiesAndRelationsByTarget(ev, 'Not-Needed', rowModel, rowModel.selectedRelatedEntity, 'relatedListOfInstancesChange');

                $mdDialog.cancel();

            }, function () { // Cancel
                // Do nothing
            });

        }

        $scope.validateChip = function ($chip, chips) {
            if (!$chip)
                return;
            // check if the current string length is greater than or equal
            // to a character limit.
            if ($chip.name.length < 4) {
                $scope.message = 'Keyword length must be greater than 3 characters.';
                $scope.showErrorAlert('Information', $scope.message);                 // remove
                // the
                // last
                // added
                // item.
                chips.pop();
            }
        }
        $scope.clearFieldsTarget = function () {
            $scope.targetModel.numericRange = {
                from: null,
                to: null,
            };
            $scope.targetModel.timePrimitive = {
                from: null,
                to: null,
            };
            $scope.targetModel.targetChips = [];
            $scope.targetModel.rangeOfDates = null;
            $scope.targetModel.booleanValues = null;

        }

        $scope.clearFieldsRelated = function (rowModel) {
            console.log($scope.rowModelList)
            rowModel.numericRange = {
                from: null,
                to: null,
            };
            rowModel.timePrimitive = {
                from: null,
                to: null,
            };
            rowModel.relatedChips = [];
            rowModel.rangeOfDates = null;
            rowModel.booleanValues = null;

        }



        $scope.getResultUri = function (item, index) {

            var uri = "";
            var i = 0;
            for (key in item) {

                if (item.hasOwnProperty(key)) {
                    if (item[key].type == "uri") {
                        if (i < index) {
                            uri = item[key].value;
                        }
                    }
                }
                i++;
            }

            return uri;
        }

        $scope.countVariables = function (variable) {

            if (variable != undefined) {
                return (variable.split(" ")).length;
            }
        }

        $scope.downloadCSV = function (variable) {

            console.log("called download");
            var modalOptions = {
                headerText: 'Loading Please Wait...',
                bodyText: 'Preparing CSV file...'
            };
            var modalInstance = modalService.showModal(modalDefaults, modalOptions);

            var req = new XMLHttpRequest();
            req.onreadystatechange = function () {
                if (this.readyState == 4 && this.status == 200) {
                    console.log(" Finish");
                    modalInstance.close();
                }
            };
            req.open("POST", "/aqub/export_to_csv", true);
            req.responseType = "blob";
            req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

            req.onload = function (event) {
                var blob = new Blob([req.response], {type: 'text/csv'});
                var fileName = "export.csv"
                var link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = fileName;
                link.click();
            };
            req.send();
        }


        $scope.goToPage = function (link) {

            $window.open(link, '_blank');

        }
        /*
         * angular.forEach(list, function(value, key) { //if() //exists =
         * $log.info('value: ' + value); });
         * 
         * Promise.resolve(initRowModels()).then(function() { //initPurpose =
         * false; });
         */

        $scope.isImage = function (imageName) {
            var image_ext = ["gif", "jpg", "jpeg", "png", "tiff", "tif"];
            var re = /(?:\.([^.]+))?$/;
            var ext = re.exec(imageName)[1];
            if (ext !== undefined) {
                ext = ext.toLowerCase();
                return image_ext.includes(ext);
            }
            return false;

        }


    }]);
