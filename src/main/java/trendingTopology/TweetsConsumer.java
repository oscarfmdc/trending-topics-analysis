package trendingTopology;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TweetsConsumer extends BaseRichSpout{	

	private static final long serialVersionUID = 1L;
	private SpoutOutputCollector collector;
	private static final ObjectMapper objectMapper = new ObjectMapper();
	KafkaConsumer<String, String> kafkaConsumer;

	@Override
	public void nextTuple() {        
		ConsumerRecords<String, String> records = kafkaConsumer.poll(10);
		for (ConsumerRecord<String, String> record : records) {
			
			JsonNode root;
			try {
				root = objectMapper.readTree(record.value());
				JsonNode hashtagsNode = root.path("entities").path("hashtags");
				String language = record.key();
				if (language != null && !hashtagsNode.toString().equals("[]")) {
					for (JsonNode node : hashtagsNode) {
						String hashtag = node.path("text").asText();
					}
					collector.emit("tweetsStream", new Values(new String(record.value())));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void open(Map arg0, TopologyContext arg1, SpoutOutputCollector collector) {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put("group.id", "MYGROUP");
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		this.kafkaConsumer = new KafkaConsumer<>(props);
		this.collector = collector;
		kafkaConsumer.subscribe(Arrays.asList("Tweets"));
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("tweetsStream", new Fields("tweet"));
	}
}
