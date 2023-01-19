
app.controller("importCtrl", ['$scope', '$sessionStorage', 'queryService', 'importService', '$mdDialog', 'authenticationService', '$state', '$mdSidenav', '$mdToast',
    function ($scope, $sessionStorage, queryService, importService, $mdDialog, authenticationService, $state, $mdSidenav, $mdToast) {
        // Toggles SidePanel
        $scope.toggleInfo = buildToggler('rightInfo');

        function buildToggler(componentId) {
            return function () {
                $mdSidenav(componentId).toggle();
            };
        }

        $scope.headingTitle = "Import data from file(s)";

        // Calling service to get the user's credentials (token, userId)
        function initCredentials() {
            $scope.credentials = authenticationService.getCredentials();
            if ($scope.credentials == undefined) {
                $state.go('login', {});
            }
        }
        initCredentials();

        // Regarding roles
        $scope.userProfile = authenticationService.getUserProfile();
        $scope.isAdmin = function () {

            if ($scope.userProfile != null) {
                if ($scope.userProfile.role != null) {
                    if ($scope.userProfile.role == 'ADMIN')
                        return true;
                    else
                        return false;
                } else
                    return false;
            } else
                return false;
        }

        // If not admin then redirect to welcome
        function checkIfAllowedByRole() {
            if (!$scope.isAdmin()) {
                $state.go('welcome', {});
            }
        }
        checkIfAllowedByRole();


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
                    .ariaLabel('Alert Dialog Demo')
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

        // Alert (danger, warning, success)
        $scope.alerts = [];
        $scope.importErrorAlerts = [];
        $scope.importSuccessAlerts = [];

        $scope.namedGraphs = [];
        $scope.namedGraphTree = [];
        $scope.selectedCategory = {label: null, id: null};

        $scope.fileCount = 0; // Total count of files to be processed
        $scope.progressValue = 0; // Initial progress indication
        $scope.processStarted = false; // Flag indicating that the uploading/file-processing has started


        // Initializing All available named graphs
        function initNamedGraphs(newCase) {

            queryService.getAllNamegraphs().then(function (response) {
                if (response.status == '200') {
                    // Holding the whole tree for future usage
                    $scope.namedGraphTree = response.data;
                    $scope.namedGraphs = []; // Clearing the list of named graphs
                    // response.data is an array of children. Each children is an array of named graphs
                    for (var i = 0; i < response.data.length; i++) {
                        for (var j = 0; j < response.data[i].children.length; j++) {
                            $scope.namedGraphs.push(response.data[i].children[j]);
                        }
                    }
                    console.log();
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

        initNamedGraphs();

        /**
         * Search for namedGraphs... use $timeout to simulate
         * remote dataservice call.
         */
        $scope.querySearch = function (query) {
            var results = query ? $scope.namedGraphs.filter(createFilterFor(query)) : $scope.namedGraphs, deferred;
            return results;
        }

        /**
         * Create filter function for a query string
         */
        function createFilterFor(query) {
            var lowercaseQuery = angular.lowercase(query);

            return function filterFn(state) {
                return (angular.lowercase(state.label).indexOf(lowercaseQuery) === 0);
            };

        }

        $scope.selectedFormat = 'Automatic';
        $scope.formats = [
            'Automatic',
            'application/rdf+xml',
            'text/rdf+n3',
            'text/plain',
            'application/x-turtle',
            'application/ld+json',
            'application/x-trig',
            'text/x-nquads'
        ];

        $scope.setFormat = function (format) {
            $scope.selectedFormat = format;
        };

        $scope.setSearchText = function (text) {
            $scope.searchText = text;
        };

        // Configuration for the fileAppDirective
        $scope.dropzoneConfig = {

            /*
             rdf, rdfs, owl -> RDFXML
             nt -> NTRIPLES
             n3 -> N3
             ttl -> TURTLE
             trig -> TRIG
             trix -> TRIX
             */
            options: {// passed into the Dropzone constructor
                url: '/upload',
                uploadMultiple: true,
                paramName: "uploadfile", // The name that will be used to transfer the file
                params: {'contentTypeParam': $scope.selectedFormat, 'authorizationParam': $scope.credentials.token},
                maxFilesize: 400, // MB
                maxThumbnailFilesize: 10,
                parallelUploads: 1, //number of parallel uploads
                acceptedFiles: '.rdfs,.rdf,.owl,.nt,.n3,.nt,.ntriples,.ttl,.jsonld,.trig,.trix',
                autoProcessQueue: false,
                maxFiles: 100//,
                        //method: "POST"
            },
            'eventHandlers': {
                'init': function () {
                    this.options.acceptedFiles = $scope.acceptedFile;
                    $scope.$apply(function () {
                        $scope.alerts.splice(0);
                        $scope.importErrorAlerts.splice(0);
                        $scope.importSuccessAlerts.splice(0);
                    });
                },
                'addedfile': function (file, response) {
                    $scope.file = file;

                    $scope.$apply(function () {
                        $scope.fileAdded = true;
                    });

                },
                'processing': function (file) {

                    // Named graph is required
                    if ($scope.selectedNamedGraph != null) {

                        // Setting additional parameter dynamically 
                        // The parameter holds the content-type (i.e. application/rdf+xml)
                        if ($scope.selectedFormat == 'Automatic') {
                            this.options.params = {
                                'contentTypeParam': getContentTypeFromFileExtension(file.name.split('.').pop()),
                                'namedGraphIdParam': $scope.selectedNamedGraph.id,
                                'namedGraphLabelParam': $scope.selectedNamedGraph.label,
                                'selectedCategoryLabel': $scope.selectedCategory.value,
                                'selectedCategoryId': $scope.selectedCategory.id,
                                'linkingUpdateQuery': $scope.provenaceQuery,
                                'authorizationParam': $scope.credentials.token
                            };
                        } else {
                            this.options.params = {
                                'contentTypeParam': $scope.selectedFormat,
                                'namedGraphIdParam': $scope.selectedNamedGraph.id,
                                'namedGraphLabelParam': $scope.selectedNamedGraph.label,
                                'selectedCategoryLabel': $scope.selectedCategory.value,
                                'selectedCategoryId': $scope.selectedCategory.id,
                                'linkingUpdateQuery': $scope.provenaceQuery,
                                'authorizationParam': $scope.credentials.token
                            };
                        }

                    }
                },
                'success': function (file, response) {

                    $scope.$apply(function () {

                        if (response == null) {
                            /*
                             $scope.alerts.push({
                             type: 'danger-funky', 
                             msg: 'There was an internal error while trying to upload the file \"' + file.name + '\".'
                             });
                             */
                            $mdToast.show(
                                    $mdToast.simple()
                                    .textContent('There was an internal error while trying to upload the file \"' + file.name + '\".')
                                    .position('top right')
                                    .parent(angular.element('#dialogContent'))
                                    .action('OK')
                                    .highlightAction(true)
                                    .hideDelay(10000)
                                    );
                            $scope.importErrorAlerts.push({
                                type: 'danger-funky',
                                msg: 'There was an internal error while trying to upload the file \"' + file.name + '\".',
                                titleType: 'Error'
                            });

                            $scope.determinateValue = $scope.determinateValue

                        } else { // Everything went fine

                            /*
                             $scope.alerts.splice(0);
                             $scope.alerts.push({
                             type: 'success-funky', 
                             msg: 'File \"' + file.name + '\" was imported successfully in ' + JSON.parse(response.message).data.milliseconds + ' milliseconds.'
                             });
                             */

                            //.textContent('File \"' + file.name + '\" was imported successfully in ' + JSON.parse(response.message).data.milliseconds + ' milliseconds.')
                            $mdToast.show(
                                    $mdToast.simple()
                                    .textContent('File \"' + file.name + '\" was imported successfully.')
                                    .position('top right')
                                    .parent(angular.element('#dialogContent'))
                                    .action('OK')
                                    .highlightAction(true)
                                    .highlightClass('md-primary')
                                    .theme("altTheme")
                                    .hideDelay(10000)
                                    );
                            $scope.importSuccessAlerts.push({
                                type: 'success-funky',
                                msg: 'File \"' + file.name + '\" was imported successfully!', // in ' + JSON.parse(response.message).data.milliseconds + ' milliseconds.',
                                titleType: 'Success'
                            });
                        }

                        $scope.file = null;

                    });

                    //////this.removeFile(file);

                    if (this.getUploadingFiles().length === 0 && this.getQueuedFiles().length === 0) {
                        $scope.alerts.splice(0);

                        // Two cases: either "only success" or "success and errors". 
                        // Errors without a single success is not a case. Remember that you are in the success response handler.
                        if ($scope.importErrorAlerts.length === 0) {
                            $scope.alerts.push({
                                type: 'success-funky',
                                msg: 'All files have been successfully imported!',
                                showDetails: true
                            });
                        }
                    }
                    ;

                },

                'complete': function (file, response) {

                    ////$scope.progressValue = 100 * (($scope.fileCount-$scope.getDropzoneAcceptedFiles()) / $scope.fileCount);
                    /*
                     if (this.getUploadingFiles().length === 0 && this.getQueuedFiles().length === 0) {
                     }
                     */
                },

                'queuecomplete': function (files) {
                    $scope.$apply(function () {
                        $scope.alerts.splice(0);

                        // Show warning message when there are both errors and success messages
                        if ($scope.importSuccessAlerts.length > 0 && $scope.importErrorAlerts.length > 0) {
                            $scope.alerts.push({
                                type: 'warning-funky',
                                msg: '<b>Warning</b> - ' + $scope.importSuccessAlerts.length + ' files have been successfully imported! However errors occured on ' + $scope.importErrorAlerts.length + ' files!',
                                showDetails: true
                            });
                        } else if ($scope.importSuccessAlerts.length <= 0 && $scope.importErrorAlerts.length > 0) {
                            $scope.alerts.push({
                                type: 'danger-funky',
                                msg: '<b>Error</b> - ' + $scope.importErrorAlerts.length + ' file(s) had errors!',
                                showDetails: true
                            });
                        } else if ($scope.importSuccessAlerts.length > 0 && $scope.importErrorAlerts.length <= 0) {
                            $scope.alerts.push({
                                type: 'success-funky',
                                msg: '<b>Success</b> - All ' + $scope.importSuccessAlerts.length + 'files have successfully been imported.',
                                showDetails: true
                            });
                        }

                        // Remove all files
                        $scope.resetDropzone();

                        // Calling materialization service
                        //materializeMetadata();

                        // Show errors
                        /*
                         angular.forEach($scope.importErrorAlerts, function(value, key) {
                         $scope.alerts.push({type: value.type, msg: value.msg});
                         });
                         */
                    });



                    // Refreshing NamedGraphs list for the auto-complete
                    initNamedGraphs();

                    // Setting flag to indicate that the process has finished
                    $scope.processStarted = false;
                },

                'successmultiple': function (files, response) {
                    if (files.length > 1) {
                        $scope.$apply(function () {
                            $scope.alerts.splice(0);
                            $scope.alerts.push({
                                type: 'success-funky',
                                msg: 'All files have successfully been imported.'
                            });
                        });
                    }
                    /*
                     for (var i=0; i<files.length; i++) {
                     this.removeFile(files[i]);
                     }
                     */
                },

                'completemultiple': function (files, response) {

                    this.processQueue();
                    //alert("all good")
                },

                'error': function (file, response) {
                    //console.log(response);

                    $scope.$apply(function () {
                        if (response != null) {
                            /*
                             $scope.alerts.splice(0);
                             $scope.alerts.push({
                             type: 'danger-funky', 
                             msg: 'Parsing errors occurred while trying to process the file \"' + file.name + '\".'
                             });
                             */
                            $mdToast.show(
                                    $mdToast.simple()
                                    .textContent('Parsing errors occurred while trying to process the file \"' + file.name + '\".')
                                    .position('top right')
                                    .parent(angular.element('#dialogContent'))
                                    .action('OK')
                                    .highlightAction(true)
                                    .hideDelay(10000)
                                    );
                            $scope.importErrorAlerts.push({
                                type: 'danger-funky',
                                msg: 'Parsing errors occurred while trying to process the file \"' + file.name + '\".',
                                titleType: 'Error'
                            });
                        }
                    });
                },

                'totaluploadprogress': function (progress) {

                    //$scope.progressValue = progress;
                }

            }
        };

        $scope.categories = [];

        // Initializing the list of categories of the named graphs 
        // with respect to id and label
        $scope.initCategories = function () {
            if ($scope.categories.length <= 0) {
                angular.forEach($scope.namedGraphTree, function (value, key) {
                    $scope.categories.push({value: value.label, id: value.id});
                });
            }
        }

        $scope.closeNamedGraphSelectCategoryDialog = function () {
            $scope.selectedCategory = {label: null, id: null};
            $mdDialog.cancel();
        }

        // Called when the "Import Data" button is clicked
        $scope.uploadFile = function (ev) {

            // Clearing messages
            $scope.resetMessages();
            // Initializing the progress indication value
            $scope.progressValue = 0;
            // Getting the total file count
            $scope.fileCount = angular.copy($scope.getDropzoneAcceptedFiles());

            // Checking if it is new namedGraph
            if ($scope.selectedNamedGraph == null) {
                $scope.selectedNamedGraph = {id: null, label: $scope.searchText}

                // Prompting dialog to select category for the new named graph
                $mdDialog.show({
                    scope: $scope,
                    templateUrl: 'views/dialog/selectNamedGraphCategory.tmpl.html',
                    parent: angular.element(document.body),
                    targetEvent: ev,
                    preserveScope: true,
                    fullscreen: false // Only for -xs, -sm breakpoints.
                });

            } else {
                // Calling service for handling user profile data before starting the upload
                insertUserProfileMetadata(ev);
            }
        };

        // Called when pressing the 'continue' button from the 
        // dialog shown when selecting category
        $scope.continueAfterSelectingCategory = function (ev) {

            // Clearing messages
            $scope.resetMessages();
            // Initializing the progress indication value
            $scope.progressValue = 0;
            // Getting the total file count
            $scope.fileCount = angular.copy($scope.getDropzoneAcceptedFiles());
            // Setting flag to indicate that the process has started
            $scope.processStarted = true;
            insertMetadata(ev); // Inserting meta-data for this uploading procedure
            // Close the dialog
            $scope.closeNamedGraphSelectCategoryDialog();
        }

        function insertMetadata(ev) {

            var importModel = {
                namedGraphLabelParam: $scope.selectedNamedGraph.label,
                namedGraphIdParam: $scope.selectedNamedGraph.id,
                selectedCategoryLabel: $scope.selectedCategory.value,
                selectedCategoryId: $scope.selectedCategory.id
            }

            importService.createMetadataInfo(angular.toJson(importModel), $scope.credentials.token)
                    .then(function (response) {

                        if (response.status == '200') {
                            if (response.data.success == true) {
                                $mdToast.show(
                                        $mdToast.simple()
                                        .textContent(response.data.message)
                                        .position('top right')
                                        .parent(angular.element('#dialogContent'))
                                        .hideDelay(3000)
                                        );

                                $scope.selectedNamedGraph.id = response.data.namedGraphIdParam;

                                // Insert user profile meta-data into the triple-store
                                insertUserProfileMetadata(ev);

                            } else {
                                $scope.message = response.data.message;
                                $scope.showErrorAlert('Error', $scope.message);
                                $scope.searchText = ''; // Initialize named graph input
                                // Setting flag to indicate that the process has been stopped
                                $scope.processStarted = false;
                            }
                        } else if (response.status == '400') {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                            // Setting flag to indicate that the process has been stopped
                            $scope.processStarted = false;
                        } else if (response.status == '401') {
                            $log.info(response.status);
                            $scope.showLogoutAlert();
                            authenticationService.clearCredentials();
                            // Setting flag to indicate that the process has been stopped
                            $scope.processStarted = false;
                        } else {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                            // Setting flag to indicate that the process has been stopped
                            $scope.processStarted = false;
                        }

                    }, function (error) {
                        $scope.message = 'There was a network error. Try again later.';
                        alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                            data: error
                        }));
                        // Setting flag to indicate that the process has been stopped
                        $scope.processStarted = false;
                    });

        }

        // Calling service to add user profile metadata in the triplestore if not already there
        function insertUserProfileMetadata(ev) {

            var userProfileDataModel = angular.copy($scope.userProfile);
            userProfileDataModel.namedGraphId = angular.copy($scope.selectedNamedGraph.id);
            userProfileDataModel.namedGraphLabel = angular.copy($scope.selectedNamedGraph.label);

            importService.insertUserProfileMetadataInfo(angular.toJson(userProfileDataModel), $scope.credentials.token)
                    .then(function (response) {

                        if (response.status == '200') {
                            if (response.data.success == true) {
                                $mdToast.show(
                                        $mdToast.simple()
                                        .textContent(response.data.message)
                                        .position('top right')
                                        .parent(angular.element('#dialogContent'))
                                        .hideDelay(3000)
                                        );

                                // Initiating the process (file upload & processing)

                                // Setting flag to indicate that the process has started
                                $scope.processStarted = true;

                                // Initiating the process
                                //if(response.data.linkingUpdateQuery != null) {
                                $scope.provenaceQuery = response.data.linkingUpdateQuery;
                                $scope.processDropzone();
//		        	}
//		        	else {
//						$scope.message = "Provenance information is missing";
//						$scope.showErrorAlert('Error', $scope.message);
//						$scope.searchText = ''; // Initialize named graph input
//						// Setting flag to indicate that the process has been stoped
//				    	$scope.processStarted = false;
//					}


                            } else {
                                $scope.message = response.data.message;
                                $scope.showErrorAlert('Error', $scope.message);
                                $scope.searchText = ''; // Initialize named graph input
                                // Setting flag to indicate that the process has been stoped
                                $scope.processStarted = false;
                            }
                        } else if (response.status == '400') {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                            // Setting flag to indicate that the process has been stoped
                            $scope.processStarted = false;
                        } else if (response.status == '401') {
                            $log.info(response.status);
                            $scope.showLogoutAlert();
                            authenticationService.clearCredentials();
                            // Setting flag to indicate that the process has been stoped
                            $scope.processStarted = false;
                        } else {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                            // Setting flag to indicate that the process has been stoped
                            $scope.processStarted = false;
                        }

                    }, function (error) {
                        $scope.message = 'There was a network error. Try again later.';
                        alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                            data: error
                        }));
                        // Setting flag to indicate that the process has been stoped
                        $scope.processStarted = false;
                    });

        }

        // Materialization Service to be called after all uploads have been completed
        function materializeMetadata() {

            console.log("Materialization Process started");

            var materializationModel = {
                linkingUpdateQuery: $scope.provenaceQuery,
                namedGraphIdParam: $scope.selectedNamedGraph.id,
                namedGraphLabel: $scope.selectedNamedGraph.label
            }

            importService.materializeNewData(angular.toJson(materializationModel), $scope.credentials.token)
                    .then(function (response) {

                        if (response.status == '200') {
                            if (response.data.success == true) {
                                $mdToast.show(
                                        $mdToast.simple()
                                        .textContent(response.data.message)
                                        .position('top right')
                                        .parent(angular.element('#dialogContent'))
                                        .hideDelay(3000)
                                        );

                            } else {
                                $scope.message = response.data.message;
                                $scope.showErrorAlert('Error', $scope.message);
                            }
                        } else if (response.status == '400') {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                            // Setting flag to indicate that the process has been stopped
                            $scope.processStarted = false;
                        } else if (response.status == '401') {
                            $log.info(response.status);
                            $scope.showLogoutAlert();
                            authenticationService.clearCredentials();
                            // Setting flag to indicate that the process has been stopped
                            $scope.processStarted = false;
                        } else {
                            $log.info(response.status);
                            $scope.message = 'There was a network error. Try again later and if the same error occures again please contact the administrator.';
                            $scope.showErrorAlert('Error', $scope.message);
                            // Setting flag to indicate that the process has been stopped
                            $scope.processStarted = false;
                        }

                    }, function (error) {
                        $scope.message = 'There was a network error. Try again later.';
                        alert("failure message: " + $scope.message + "\n" + JSON.stringify({
                            data: error
                        }));
                        // Setting flag to indicate that the process has been stopped
                        $scope.processStarted = false;
                    });

        }


        $scope.reset = function () {
            $scope.file = null;
            $scope.setFormat('Automatic');
            $scope.setSearchText('');
            $scope.resetDropzone();
            $scope.alerts.splice(0);
            $scope.importErrorAlerts.splice(0);
            $scope.importSuccessAlerts.splice(0);
        };

        $scope.resetMessages = function () {
            $scope.alerts.splice(0);
            $scope.importErrorAlerts.splice(0);
            $scope.importSuccessAlerts.splice(0);
        };

        function getContentTypeFromFileExtension(fileExtension) {

            if (fileExtension == 'rdfs' || fileExtension == 'rdf' || fileExtension == 'owl') {
                return "application/rdf+xml";
            } else if (fileExtension == 'n3') {
                return "text/rdf+n3";
            } else if (fileExtension == 'nt' || fileExtension == 'ntriples') {
                return "text/plain";
            } else if (fileExtension == 'ttl') {
                return "application/x-turtle";
            } else if (fileExtension == 'trig') {
                return "application/x-trig";
            } else if (fileExtension == 'trix') {
                return "text/x-nquads";
            } else if (fileExtension == 'jsonld') {
                return "application/ld+json";
            } else {
                return "text/plain";
            }
        }

        $scope.showAdvanced = function (ev) {
            $mdDialog.show({
                //controller: 'importCtrl',
                scope: $scope,
                templateUrl: 'views/dialog/importStatusFileList.html',
                parent: angular.element(document.body),
                targetEvent: ev,
                clickOutsideToClose: true,
                preserveScope: true,
                fullscreen: false // Only for -xs, -sm breakpoints.
            });

        };

    }]);

