/*
 * View handles the views for the dash board.  Each defined View subclass
 * needs to implement the interface functions to update the banner, controls, main
 * and footer div of the dash board.
 */

// View constructor
function View() {

}

// Logger for the View
View._logger = log4javascript.getLogger();


// configure_html - Read in JSON describing each specific view.  Build
// the banner, controls, main and footer.  Assume no data has yet been loaded.
View.prototype.configure_html = function(json_config) {
	throw new Error('View.configure_html is not defined!');
}

// load_data - Loads in a new DataSource with which to display data
View.prototype.load_data = function(data_source) {
	throw new Error('View.load_data is not defined!');
}

// loading - Pass true/false to enable/disable the View's loading graphic
View.prototype.loading = function(enable) {
	throw new Error('View.loading is not defined!')
}


/*
 * ViewGraph - Create a View to show graphs of user data
 */
function ViewGraph() {
	
}

ViewGraph.prototype = new View();


// ViewGraph.configure_html - Give JSON that describes the surveys and prompts.
ViewGraph.prototype.configure_html = function(json_config) {
	// First setup the main panel
	
    var cur_group = -1;
    
    // Loop over each graph type
    json_config.forEach(function(config) {
        // If we are in a new group, add a new pane to the tabs
        if (cur_group != config.group_id) {
            // Grab new group name from the group_list
            var new_group_name = group_list[config.group_id];
            // Translate the name into something that works as an html reference
            var new_group_name_ref = new_group_name.toLowerCase().replace(' ', '_');
            
            if (View._logger.isDebugEnabled()) {
                View._logger.debug("Creating group name: " + new_group_name + " with ref: " + new_group_name_ref);
            }
            
            // Make this specific to the Main panel
            $('.tabs').append('<li><a href="' + new_group_name_ref + '">' + new_group_name + '</a></li>');
            $('.panes').append('<div id="group_' + config.group_id + '"></div>');
            
            cur_group = config.group_id;
        }
        
        // Now append a new div into the panes for our new graph
        $('.panes > #group_' + cur_group).append('<div class="ProtoGraph" id="prompt_' + config.prompt_id + '"></div>');
    
        // Create a unique div ID for Protovis to know where to attach the graph
        var div_id = 'ProtoGraph_' + cur_group + '_' + config.prompt_id;
        
        // Put the graph title and another div for the graph itself into this div
        $('#group_' + cur_group + ' > #prompt_' + config.prompt_id)
            .append('<span class="graph_title">' + config.text + '</span>')
            .append('<div id="' + div_id + '"></div>');
        
        // Finally create a new graph and add it to the div
        // Make the graph have the width of the tab panes
        var new_graph = ProtoGraph.factory(config, div_id, $('div.panes').width());
        $('#' + div_id)
            .data('graph', new_graph)
            .data('prompt_id', config.prompt_id)
            .data('group_id', cur_group)
            .data('hidden', false);
    });
    
    // setup ul.tabs to work as tabs for each div directly under div.panes 
    $("ul.tabs").tabs("div.panes > div");
    
    // Hide all the graphs for now
    $('div.ProtoGraph').hide();
    
    // Append a loading div in the pane
    $('div.panes > div').append('<div class="loading"></div>');
}


ViewGraph.prototype.load_data = function(data_source) {
    // Iterate over every ProtoGraph class
    var that = this;
    $('div.ProtoGraph > div').each(function(index) {
        // Grab the graph object attached to this div
        var graph = $(this).data('graph');
        var prompt_id = $(this).data('prompt_id');
        var group_id = $(this).data('group_id');
        
        // Time the rendering of the graph
        if (View._logger.isDebugEnabled()) {
        	View._logger.debug("Rendering graph with prompt_id " + prompt_id + " group_id " + group_id);
            var start_render_time = new Date().getTime();
        }
        
        // Grab data for the specified prompt/group
        try {
            // Hack in custom graphs here
            if (graph instanceof ProtoGraphCustomSleepType) {
                var new_data = data_source.retrieve_data_sleep_time();
            }
            else if (graph instanceof ProtoGraphMultiTimeType) {
                var new_data = data_source.retreive_data_saliva();
            }
            // No custom data processing
            else {
                var new_data = data_source.retrieve_data(prompt_id, group_id);
            }
            
            
        }
        catch (error) {
            if (error instanceof DataSourceJson.NoDataError) {
                if (View._logger.isInfoEnabled()) {
                	View._logger.info(error.message);
                }
                
                // Replace graph with no data found warning
                if ($(this).data('hidden') == false) {
                    that.replace_with_no_data($(this));
                    $(this).data('hidden', true);
                }
            }
            
            return;
        }
        
        if (View._logger.isDebugEnabled()) {
        	View._logger.debug("Found " + new_data.length + " data points");
        }
        
        // If the graph was hidden due to no data found, unhide
        if ($(this).data('hidden') == true) {
            that.replace_with_graph($(this));
            $(this).data('hidden', false);
        }
        
        // Apply data to the graph
        graph.apply_data(new_data, 
                         startDate, 
                         numDays);
        
        // Re-render graph with the new data
        graph.render();
        
        
        if (View._logger.isDebugEnabled()) {
            var time_to_render = new Date().getTime() - start_render_time;           
            View._logger.debug("Time to render graph: " + time_to_render + " ms");
        }               
    });
}

ViewGraph.prototype.loading = function(enable) {
    if (enable) {
        // Hide the graphs while loading
        $("div.ProtoGraph").hide();
        // Show the loading graphic in the displayed pane
        $('div.panes .loading').show();
    }
    else {
        // Hide all the loading divs in the panes
        $('div.panes .loading').hide();
        // And reshow the graphs
        $("div.ProtoGraph").show();
    }
}

// Hide the passed div and add a no data found
ViewGraph.prototype.replace_with_no_data = function(div_to_replace) {
    div_to_replace.after("<span>No data found</span>");
    div_to_replace.hide();
}

// Show the passed div and remove the next sibling
ViewGraph.prototype.replace_with_graph = function(div_to_show) {
    div_to_show.next().remove();
    div_to_show.show();
}