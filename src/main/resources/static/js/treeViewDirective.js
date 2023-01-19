
app.directive('mdBox', function() {
	return {
		restrict: 'AE',
		//require: '^ivhTreeview',
		template: [
		    '<span class="ascii-box">',
	        	'<span ng-show="node.selected" class="x"><md-checkbox aria-label="checked" ng-checked="true"></md-checkbox></span>',
	        	'<span ng-show="node.__ivhTreeviewIndeterminate" class="y"><md-checkbox aria-label="checked" ng-checked="false"></md-checkbox></span>',
	        	'<span ng-hide="node.selected || node.__ivhTreeviewIndeterminate"><md-checkbox aria-label="checked" ng-checked="false"></md-checkbox></span>',
	      	'</span>',
		].join(''),
		link: function(scope, element, attrs) {
		      element.on('click', function() {
		        scope.trvw.toggleSelected(scope.node);
		        //scope.$apply();
		      });
		}
	};
});

/*
app.directive('mdBox', function(ivhTreeviewMgr) {
	return {
		restrict: 'AE',
		//require: '^ivhTreeview',
		template: [
			'<span class="ascii-box">',
				'<span ng-show="node.selected" class="x"><md-checkbox style="min-height: 100%; line-height: 0" aria-label="checked" ng-checked="true"></md-checkbox></span>',
				'<span ng-show="node.__ivhTreeviewIndeterminate" class="y"><md-checkbox style="min-height: 100%; line-height: 0" aria-label="checked" ng-checked="false"></md-checkbox></span>',
				'<span ng-hide="node.selected || node.__ivhTreeviewIndeterminate"><md-checkbox style="min-height: 100%; line-height: 0" aria-label="checked" ng-checked="false"></md-checkbox></span>',
			'</span>',
		].join(''),
		link: function(scope, element, attrs) {
			element.on('click', function() {
				//ivhTreeviewMgr.select(stuff, scope.node, !scope.node.selected);
				ivhTreeviewMgr.select(scope.node, !scope.node.selected);
				scope.trvw.toggleSelected(scope.node);
				//scope.trvw.select(scope.node);
				
				console.log("status: " + scope.node.selected);
				scope.$apply();
			});
		}
	};
});
*/