package org.tudelft.graphalytics.mapreducev2.conn;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.tudelft.graphalytics.mapreducev2.conn.ConnectedComponentsConfiguration.LABEL_STATUS;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author Marcin Biczak
 */
public class LabelDirectedConnectedComponentsReducer extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterator<Text> values,
                       OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            Vector<String> potentialLabels = new Vector<String>();

            String link = "";
            String theTag = "Z";
            int i = 0;

            while (values.hasNext()) {
                Text value = values.next();

                if(value.toString().startsWith("$"))
                    link = value.toString();
                else if(value.toString().compareTo(theTag) < 0) {
                    theTag = value.toString();
                    potentialLabels.add(value.toString());
                }
                
                //report progress
                if(i % 1000 == 0) reporter.progress();
                i++;
            }

            output.collect(key, new Text(theTag+link));
            reporter.progress();

            i = 0;
            Iterator<String> iter = potentialLabels.iterator();
            while (iter.hasNext()) {
                String prev = iter.next();
                if(!theTag.equals(prev)) {
                    reporter.incrCounter(LABEL_STATUS.UPDATED, 1);
                }

                //report progress
                if(i % 1000 == 0) reporter.progress();
                i++;
            }
        }
    }
