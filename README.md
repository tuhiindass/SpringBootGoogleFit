# SpringBootGoogleFit
<ul>
	<li>Phase 1:</li>
At the starting of this project, we implemented all the google fit api which includes following:
	<ol type='a'>
	<li> Api for fetch all the raw DataSources</li>
	<li> APi for List of DataSources with a link which would redirect to the data for each data source.</li>
	<li> Api for all DataSets across DataSources</li>
	<li> Api for all DataPoint changes across DataSource</li>
	<li> Api for all Datasets Using Aggregate accross DataSource from Midnight to Current time.Also fetch the logged in users details by using google api. </li>
	</ol>

<li>Phase 2:</li>
Add a new api which will filter all thhe DataSources based on a value ("top_level") which would print the all different kind of Activity types and print them in browser so that it would be easier for the user to understand and on clicking on those activity type, we return the data for those activity type only.
	<ol type='a'>
	<li> Api for List of Activity type </Li>
	</ol>

<li>Phase 3:</li>
In phase 3, we resolve the following issue,
	<ol type='a'>
	<li> Multiple user's login issue. </li>

	
On multiple users login, some users are getting data for other users which is causing the data security issue. Resolve this by introduce Session using Cookie.
	

	
<li>Duplicate Activity Type. </li>
	
For some user, in the Activity type list, data is coming with duplicate and in some in user non-understandable format. The duplicate data issue happen because of using different divices to track some activity. Resolve that by changing the filter key.
</ol>
	
<li>Phase 4:</li>
In phase 4, we resolve the following issue,
<ol type='a'>
	<li> Aggregate all data-types into a single screen with just 4-5 metric groups.</li>
   <li> Quota exceeded for quota metric 'Requests' and limit 'Requests per minute per user' of service 'fitness.googleapis.com' for consumer 'project_number'.</li>
	<li> OutOfMemoryError: Java heap space at java.base/jdk.internal.math.</li>
   <li> Performance wise a batch of 10000 records was the most optimal while inserting into db. In order to reduce code complexity I have first fetched and then broken down the list of data in batches of 10000 records and made request to save the data.</li>
	</ol>
</ul>
   
