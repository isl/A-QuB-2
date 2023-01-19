
app.directive('dropzone',[ function(){

    return function(scope, element, attrs) {
    	
    	var config, dropzone;

        config = scope[attrs.dropzone];

        // create a Dropzone for the element with the given options
        dropzone = new Dropzone(element[0], config.options);

        // bind the given event handlers
        angular.forEach(config.eventHandlers, function (handler, event) {
          dropzone.on(event, handler);
        });
    	
        scope.processDropzone = function() {
            dropzone.processQueue();
        };

        scope.resetDropzone = function() {
            dropzone.removeAllFiles();
        }
        
        scope.getDropzoneAcceptedFiles = function() {
            return dropzone.getAcceptedFiles().length;
        }
    }
    
}]);