//
//angular.module('app.securityServices', [])
//
//        .factory('authenticationService', function ($http, $timeout, $q, $rootScope, $cookies, $sessionStorage) {
//
//            // To hold the currentUser
//            var baseRootPath = '/aqub';
//
//            var currCredentials;
//            var userProfile;
//
//            return {
//                login: function (username, password) {
//                    // Dummy Implementation
//                    // return new Promise(function(resolve, reject){
//                    //     resolve({status: "SUCCEED"});
//                    // 	currCredentials = {
//                    // 	  message: '',
//                    // 	  role: "ADMIN",
//                    // 	  status: "SUCCEED",
//                    // 	  timeLimit: "2019-09-19T09:20:39.117Z",
//                    // 	  token: ''
//                    // 	}
//                    // 	$sessionStorage.currCredentials = currCredentials;
//                    // 	$sessionStorage.authenticated = true;
//                    // 	return currCredentials;
//                    // });
//
//
//                    return $http({
//                        'url': baseRootPath + '/login',
//                        'method': 'POST',
//                        'headers': {
//                            'Content-Type': 'application/x-www-form-urlencoded',
//                        },
//                        'data': 'username=' + username + '&password=' + password
//                    }).then(function (response) {
//                        if (response.data.message === 'success') {
//                            currCredentials = {
//                                message: response.data.message,
//                                role: response.data.role,
//                                status: response.data.status,
//                                username: response.data.username,
//                                token: response.data.token
//                            }
//
//                            $sessionStorage.currCredentials = currCredentials;
//                            $sessionStorage.authenticated = true;
//
//                        } else {
//                            currCredentials = {
//                                status: "FAIL",
//                                message: "Error"
//                            }
//                        }
//                        return currCredentials;
//
//                    }, function (error) {
//                        alert("err: " + err);
//                    });
//
//
//                   
//                    /*
//                     return $http({
//                     'url' : 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/login',
//                     'method' : 'Get',
//                     
//                     'headers' : {
//                     'Content-Type' : 'application/json'
//                     },
//                     'params' : {
//                     'username' : username,
//                     'pwd' : password
//                     }
//                     }).then(function (response) {				
//                     currCredentials = response.data;
//                     $sessionStorage.currCredentials = currCredentials;
//                     $sessionStorage.authenticated = true;
//                     return response.data;
//                     },function (error) {
//                     alert("err: " + err);
//                     });
//                     */
//                },
//
//                //not used anymore
//                // loginMFA: function (username, password) {
//
//                // 	return $http({
//                // 		'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/loginmfa',
//                // 		'method': 'Get',
//
//                // 		'headers': {
//                // 			'Content-Type': 'application/json'
//                // 		},
//                // 		'params': {
//                // 			'username': username,
//                // 			'pwd': password
//                // 		}
//                // 	}).then(function (response) {
//                // 		currCredentials = response.data;
//                // 		$sessionStorage.currCredentials = currCredentials;
//                // 		$sessionStorage.authenticated = true;
//                // 		return response.data;
//                // 	}, function (error) {
//                // 		alert("err: " + err);
//                // 	});
//
//                // },
//
//                // loginMFACode: function (token, code) {
//
//                // 	return $http({
//                // 		'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/loginmfacode',
//                // 		'method': 'Get',
//
//                // 		'headers': {
//                // 			'Content-Type': 'application/json'
//                // 		},
//                // 		'params': {
//                // 			'token': token,
//                // 			'code': code
//                // 		}
//                // 	}).then(function (response) {
//                // 		currCredentials = response.data;
//                // 		$sessionStorage.currCredentials = currCredentials;
//                // 		$sessionStorage.authenticated = true;
//                // 		return response.data;
//                // 	}, function (error) {
//                // 		alert("err: " + err);
//                // 	});
//
//                // },
//
//                logout: function (authendicationToken) {
//
//                    // return $http({
//                    // 	'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/logout',
//                    // 	'method': 'Get',
//
//                    // 	'headers': {
//                    // 		'Content-Type': 'application/json'
//                    // 	},
//                    // 	'params': {
//                    // 		'token': authendicationToken
//                    // 	}
//                    // }).then(function (response) {
//                    // 	$sessionStorage.$reset();
//                    // 	return response.data;
//                    // }, function (error) {
//                    // 	alert("err: " + err);
//                    // });
//
//
//                    return $http({
//                        'url': baseRootPath + '/logout',
//                        'method': 'POST',
//
//                    }).then(function (response) {
//                        $sessionStorage.$reset();
//                        return response = {
//                            "message": "",
//                            "status": "SUCCEED"
//                        }
//
//                    }, function (error) {
//                        alert("err: " + err);
//                    });
//
//                },
//
//                // Clears credentials from stored session
//                clearCredentials: function () {
//                    // Clears everything from stored session
//                    $sessionStorage.$reset();
//                },
//
//                // Get it from session
//                getCurrentCredentials: function () {
//                    currCredentials = $sessionStorage.currCredentials;
//                    if (currCredentials) {
//                        return $q.when(currCredentials);
//                    } else {
//                        return $q.reject("NO USER");
//                    }
//                },
//
//                // Get it from session
//                getCredentials: function () {
//                    return $sessionStorage.currCredentials;
//                },
//
//                // Get it from session
//                getUserProfile: function () {
//                    return $sessionStorage.userProfile;
//                },
//
//                retrieveUserProfile: function (token, username) {
//
//                    return $http({
//                        'url': baseRootPath + '/getprofile',
//                        'method': 'POST',
//
//                        'headers': {
//                            'Content-Type': 'application/json'
//                        },
//                        'data': {
//                            'username': username
//                        },
//
//                    }
//                    ).then(function (response) {
//                        userProfile = response.data;
//                        $sessionStorage.userProfile = userProfile;
//                        return response;
//                    }, function (error) {
//                        alert("err: " + err);
//                    });
//                },
//
//                retrieveAllUserProfiles: function (token) {
//
//                    return $http({
//                        'url': baseRootPath + '/getprofiles',
//                        'method': 'POST',
//
//                        'headers': {
//                            'Content-Type': 'application/json'
//                        }
//                    }
//                    ).then(function (response) {
//                        console.log(response);
//                        return response;
//                    }, function (error) {
//                        alert("err: " + err);
//                    });
//
//
//                    // return $http({
//                    // 	'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/getprofiles',
//                    // 	'method': 'Get',
//
//                    // 	'headers': {
//                    // 		'Content-Type': 'application/json'
//                    // 	},
//                    // 	'params': {
//                    // 		'token': token
//                    // 	}
//                    // }).then(function (response) {
//                    // 	return response;
//                    // }, function (error) {
//                    // 	alert("err: " + err);
//                    // });
//                },
//
//                // Get it from session
//                isAuthenticated: function () {
//                    return $sessionStorage.authenticated;
//                },
//
//                register: function (registration) {
//
//                    return $http({
//                        'url': baseRootPath + '/registerUser',
//                        'method': 'POST',
//
//                        'headers': {
//                            'Content-Type': 'application/json'
//                        },
//                        'data': registration
//                    }
//
//                    // return $http({
//                    // 	'url' : 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/createprofile',
//                    // 	'method' : 'POST',
//
//                    // 	'headers' : {
//                    // 		'Content-Type' : 'application/json'
//                    // 	},
//                    // 	//'data' : registration
//                    // 	'data' : {},
//                    // 	'params' : registration
//                    // 	/*
//                    // 	{
//                    // 		'userid' : registration.userid,
//                    // 		'name' : registration.name,
//                    // 		'email' : registration.email,
//                    // 		'organization' : registration.organization,
//                    // 		'role' : registration.role,
//                    // 		'password' : registration.password
//                    // 	}
//                    // 	*/
//                    // }
//                    ).then(function (response) {
//                        return response.data;
//                    }, function (error) {
//                        alert("err: " + err);
//                    });
//
//                },
//
//                updateProfile: function (token, userProfile) {
//
//                    // Adding token
//                    console.log(userProfile)
//                    userProfile.token = token;
//                    return $http({
//                        'url': baseRootPath + '/updateprofile',
//                        'method': 'POST',
//
//                        'headers': {
//                            'Content-Type': 'application/json'
//                        },
//                        'data': userProfile
//                    }).then(function (response) {
//                        return response;
//                    }, function (error) {
//                        alert("err: " + err);
//                    });
//
//                    // return $http({
//                    // 	'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/updateprofile',
//                    // 	'method': 'POST',
//
//                    // 	'headers': {
//                    // 		'Content-Type': 'application/json'
//                    // 	},
//                    // 	'data': {},
//                    // 	'params': userProfile
//                    // }).then(function (response) {
//                    // 	return response;
//                    // }, function (error) {
//                    // 	alert("err: " + err);
//                    // });
//
//                },
//
//                getPortalState: function () {
//
//                    return $http({
//                        'url': baseRootPath + '/retrieve_portal_state',
//                        'method': 'POST',
//                        'headers': {
//                            'Content-Type': 'application/json'
//                        }
//                    }).then(function (success) {
//                        return success;
//                    }, function (error) {
//                        //error code
//                    });
//
//                }
//
//            }
//        });


angular.module('app.securityServices', [])

        .factory('authenticationService', function ($http, $timeout, $q, $rootScope, $cookies, $sessionStorage) {

            // To hold the currentUser
            var currCredentials;
            var userProfile;
            var baseRootPath = '/aqub';


            return {
//                login: function (username, password) {
//
//                    // Dummy Implementation
//                    return new Promise(function (resolve, reject) {
//                        resolve({status: "SUCCEED"});
//                        currCredentials = {
//                            message: '',
//                            role: "ADMIN",
//                            status: "SUCCEED",
//                            timeLimit: "2019-09-19T09:20:39.117Z",
//                            token: ''
//                        }
//                        $sessionStorage.currCredentials = currCredentials;
//                        $sessionStorage.authenticated = true;
//                        return currCredentials;
//                    });
                login: function (username, password) {
                    // Dummy Implementation
                    // return new Promise(function(resolve, reject){
                    //     resolve({status: "SUCCEED"});
                    // 	currCredentials = {
                    // 	  message: '',
                    // 	  role: "ADMIN",
                    // 	  status: "SUCCEED",
                    // 	  timeLimit: "2019-09-19T09:20:39.117Z",
                    // 	  token: ''
                    // 	}
                    // 	$sessionStorage.currCredentials = currCredentials;
                    // 	$sessionStorage.authenticated = true;
                    // 	return currCredentials;
                    // });


                    return $http({
                        'url': baseRootPath + '/toLogin',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/x-www-form-urlencoded',
                        },
                        'data': 'username=' + username + '&password=' + password
                    }).then(function (response) {
                        if (response.data.message === 'success') {
                            currCredentials = {
                                message: response.data.message,
                                role: response.data.role,
                                status: response.data.status,
                                username: response.data.username,
                                token: response.data.token
                            }

                            $sessionStorage.currCredentials = currCredentials;
                            $sessionStorage.authenticated = true;
                        } else {
                            currCredentials = {
                                status: "FAIL",
                                message: "Error"
                            }
                        }
                        return currCredentials;

                    }, function (error) {
                        alert("err: " + err);
                    });


                },

                loginMFA: function (username, password) {

                    return $http({
                        'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/loginmfa',
                        'method': 'Get',

                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'params': {
                            'username': username,
                            'pwd': password
                        }
                    }).then(function (response) {
                        currCredentials = response.data;
                        $sessionStorage.currCredentials = currCredentials;
                        $sessionStorage.authenticated = true;
                        return response.data;
                    }, function (error) {
                        alert("err: " + err);
                    });

                },

                loginMFACode: function (token, code) {

                    return $http({
                        'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/loginmfacode',
                        'method': 'Get',

                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'params': {
                            'token': token,
                            'code': code
                        }
                    }).then(function (response) {
                        currCredentials = response.data;
                        $sessionStorage.currCredentials = currCredentials;
                        $sessionStorage.authenticated = true;
                        return response.data;
                    }, function (error) {
                        alert("err: " + err);
                    });

                },

                logout: function (authendicationToken) {

                    return $http({
                        'url': 'http://v4e-lab.isti.cnr.it:8080/NodeService/user/logout',
                        'method': 'Get',

                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'params': {
                            'token': authendicationToken
                        }
                    }).then(function (response) {
                        alert("response");
                        console.log(response);
                        $sessionStorage.$reset();
                        return response.data;
                    }, function (error) {
                        alert("err: " + err);
                    });

                },

                // Clears credentials from stored session
                clearCredentials: function () {
                    // Clears everything from stored session
                    $sessionStorage.$reset();
                },

                // Get it from session
                getCurrentCredentials: function () {
                    currCredentials = $sessionStorage.currCredentials;
                    if (currCredentials) {
                        return $q.when(currCredentials);
                    } else {
                        return $q.reject("NO USER");
                    }
                },

                // Get it from session
                getCredentials: function () {
                    console.log("Get creditials")
                    console.log($sessionStorage.currCredentials);
                    if ($sessionStorage.currCredential == undefined) {
                        currCredentials = {
                            message: '',
                            role: "GUEST",
                            status: "SUCCEED",
                            timeLimit: "",
                            token: ''
                        }
                        $sessionStorage.currCredentials = currCredentials;
                        $sessionStorage.authenticated = true;
                        return currCredentials;
                    } else {
                        return $sessionStorage.currCredentials;
                    }

                },

                // Get it from session
                getUserProfile: function () {
                    return $sessionStorage.userProfile;
                },

                retrieveUserProfile: function (token, username) {

                    return $http({
                        'url': baseRootPath + '/getprofile',
                        'method': 'POST',

                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'data': {
                            'username': username
                        },

                    }
                    ).then(function (response) {
                        userProfile = response.data;
                        $sessionStorage.userProfile = userProfile;
                        return response;
                    }, function (error) {
                        alert("err: " + err);
                    });
                },

                retrieveAllUserProfiles: function (token) {

                    return $http({
                        'url': baseRootPath + '/getprofiles',
                        'method': 'POST',

                        'headers': {
                            'Content-Type': 'application/json'
                        }
                    }
                    ).then(function (response) {
                        console.log(response);
                        return response;
                    }, function (error) {
                        alert("err: " + err);
                    });
                },

                // Get it from session
                isAuthenticated: function () {
                    return $sessionStorage.authenticated;
                },

                register: function (registration) {

                    return $http({
                        'url': baseRootPath + '/registerUser',
                        'method': 'POST',

                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'data': registration
                    }).then(function (response) {
                        return response.data;
                    }, function (error) {
                        alert("err: " + err);
                    });

                },

                updateProfile: function (token, userProfile) {

                    console.log(userProfile)
                    userProfile.token = token;
                    return $http({
                        'url': baseRootPath + '/updateprofile',
                        'method': 'POST',

                        'headers': {
                            'Content-Type': 'application/json'
                        },
                        'data': userProfile
                    }).then(function (response) {
                        return response;
                    }, function (error) {
                        alert("err: " + err);
                    });


                },

                getPortalState: function () {

                    return $http({
                        'url': '/retrieve_portal_state',
                        'method': 'POST',
                        'headers': {
                            'Content-Type': 'application/json'
                        }
                    }).then(function (success) {
                        return success;
                    }, function (error) {
                        //error code
                    });

                }

            }
        });