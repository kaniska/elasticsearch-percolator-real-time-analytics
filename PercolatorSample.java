package percolator.sample;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.percolator.PercolatorService;

public class PercolatorSample {
	private Node node;
	private Client client;
	    
    public static void main( String[] args ) {
    	PercolatorSample app = new PercolatorSample();
    	try {
    		app.startPipeline();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private void startPipeline() throws Exception {
    	setUp();
    	// First
    	index();
    	// Next 
    	percolate();
    	tearDown();
	}

	public void setUp() throws Exception {
        node = nodeBuilder().clusterName("es.cluster1").node();
        client = node.client();
        
        try {
        	System.out.println("Creating the Index !!");
        	addBuildInfoMapping();
        }catch(org.elasticsearch.indices.IndexAlreadyExistsException ex1) {
        	System.out.println("Index already exists !!");
        	tearDown();
        	throw new Exception(ex1);
        }catch(IOException ex2) {
        	System.out.println("Failed to percolate !!");
        	throw new Exception(ex2); 
        }
    }

    public void index() throws IOException {
        BuildInfo buildInfo = addBuildInfo();
        
        System.out.println("Index a Document !!" + toJson(buildInfo));
        
        client.prepareIndex("biz_docs", "buildinfo", buildInfo.getName())
                .setSource(toJson(biz_entity))
                .setRefresh(true)  // --> refresh the index after the operation so that the document appears in search results immediately
                .execute().actionGet();
        
        System.out.println("Search the Document !!");

        SearchResponse response = client.prepareSearch("biz_docs")
                .setTypes("biz_entity")
                .setPostFilter(filterBuilder2())
                .setExplain(true)
                .execute().actionGet();

        assertThat(response.getHits().getTotalHits(), is(1L));
        
        System.out.println("Found a Single Record !!"); 
    }

    public void percolate() throws IOException {
    	
    	System.out.println("Register the Query 1 with Percolator !!"); 
    	
        // register the percolator query-1
        client.prepareIndex("biz_docs", PercolatorService.TYPE_NAME, "matcher1")
                .setSource(jsonBuilder()
                        .startObject()
                            .field("query", constantScoreQuery(filterBuilder1()))
                            .field("user_id", "abc")
                        .endObject())
                .setRefresh(true)  // // Needed when the query shall be available immediately
                .execute().actionGet();
        
        System.out.println("Register the Query 2 with Percolator !!"); 
        
        // register the percolator query-2
        client.prepareIndex("biz_docs", PercolatorService.TYPE_NAME, "matcher2")
                .setSource(jsonBuilder()
                        .startObject()
                            .field("query", constantScoreQuery(filterBuilder2()))
                            .field("user_id", "def")
                        .endObject())
                .setRefresh(true)  // // Needed when the query shall be available immediately
                .execute().actionGet();
        
        System.out.println("Percolate an incoimg document !!"); 

        // the document to percolate
        XContentBuilder docBuilder = jsonBuilder()
                .startObject()
                    .field("doc")
                        .startObject()
                        	.field("runDate", new Date())
                        	.field("salescount", 150)
                        .endObject()
                .endObject();

        // check a document against a registered query
        PercolateResponse response = client.preparePercolate()
                .setIndices("biz_docs")
                .setDocumentType("biz_entity")
                .setSource(docBuilder)
                .execute().actionGet();

        System.out.println("Found the Matching Query Id !! " +response.getMatches()[0].getId().toString()); 
        
        assertThat(response.getCount(), is(1L));
        assertThat(response.getMatches()[0].getIndex().toString(), is("biz_docs"));
        assertThat(response.getMatches()[0].getId().toString(), is("matcher2"));

        System.out.println("Fetch the Matching Query Metadata !! "); 
        
        
        GetMappingsResponse mappingsResponse = client.admin().indices().prepareGetMappings("biz_docs").get();
        Map<String, Object> properties = (Map<String, Object>) mappingsResponse.getMappings().get("biz_docs").get("buildinfo").getSourceAsMap().get("properties");
       
        for (Entry<String, Object> entry : properties.entrySet()) {
        	 System.out.println(entry.getKey() + " : " + entry.getValue()); 
		}
        
        //assertThat(((String) matchedQuery.getSource().get("user_id")), is("abc"));
       // assertThat(matchedQuery.getSource().get("query"), is(nullValue()));
    }

    private FilterBuilder filterBuilder1() {
        return boolFilter()
                .must(rangeFilter("salescount").from("100").to("200")
                );
    }
    
    private FilterBuilder filterBuilder2() {
        return boolFilter()
                .must(rangeFilter("price").from("200").to("400")
                );
    }


    private BizInfo addBizInfo() {
        BizInfo bizInfo = new BizInfo();
        bizInfo.setName("item_price_variation_check");
        bizInfo.setDuration(100);
        bizInfo.setRunDate(new Date());
        return bizInfo;
    }

    private String toJson(BuildInfo bizInfo) throws IOException {
        return jsonBuilder()
                .startObject()
                    .field("rundate", bizInfo.getRunDate())
                    .field("salescount", bizInfo.getSalesCount())
                .endObject()
                .string();
    }

    private void addBizInfoMapping() throws Exception {
        XContentBuilder mapping = jsonBuilder()
                .startObject()
                    .startObject("bizInfo")
                        .startObject("properties")
                            .startObject("rundate")
                                .field("type", "date")
                            .endObject()
                            .startObject("salescount")
                                 .field("type", "integer")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
        ////
        boolean indexExists = client.admin().indices().prepareExists("biz_docs").execute().actionGet().isExists();
        if (indexExists) {
        	client.admin().indices().prepareDelete("biz_docs").execute().actionGet();
        }
        client.admin().indices().prepareCreate("biz_docs").addMapping("bizinfo", mapping).execute().actionGet();
    }

    public void tearDown() {
    	System.out.println("Remove the Index !! ");
    	
        client.admin().indices().prepareDelete("biz_docs").execute().actionGet();

        node.close();
    }

}
