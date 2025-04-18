package org.cpuinfofetcher

import java.nio.file.Path
import java.nio.file.Files
import java.nio.charset.StandardCharsets

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import org.dflib.DataFrame
import org.dflib.csv.Csv

/**
 * Execute Generalization class of Fetchers
 * @author Josua Carl
 * @version 1.0
 * @since 1.0
 */
class SpecificationsFetcher {

    // Define common time format
    DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
    LocalDateTime localTime = LocalDateTime.now()

    // DataFrame construction parameters
    int days_until_update = 28
    List<String> standard_cols = ['product_id', 'name', 'time', 'source', 'intended_usage']


    // Check last snap of Dataframe
    static DataFrame check_snap(Path path, List<String> newColumns) {
        path = path.toAbsolutePath().normalize()
        if (Files.isRegularFile(path)) {
            return Csv.load(path)
        } else if (newColumns != null && newColumns.size() > 0) {
            return DataFrame.empty(*newColumns)
        } else {
            return DataFrame.empty()
        }
    }

    // Method for accessing time of snapshot
    int check_last_update(def df, ChronoUnit unit) {
        if (df instanceof DataFrame && df.height() > 0) {
            if (df.getColumnsIndex().toArray().contains('time')) {
                return LocalDateTime.parse((String) df.get('time', 0), timeFormat)
                    .until(this.localTime.now(), unit)
            }
        }
        return -1
    }

    // Method to add time, source, and intended use metadata to DataFrame
    DataFrame add_metadata(DataFrame df, String source, String intendedUsage = "unknown") {
        // Create a new DataFrame for time and source metadata
        def meta_df = DataFrame.byArrayRow('time', 'source', 'intended_usage').appender()

        // Append metadata values for each row in the original DataFrame
        for (int i = 0; i < df.height(); i++) {
            meta_df.append(
                timeFormat.format(this.localTime.now()),  
                source,                                   
                intendedUsage                             
            )
        }
        meta_df = meta_df.toDataFrame()

        // Concatenate the added columns with the original DataFrame
        return meta_df.hConcat(df).colsExcept(c -> c.endsWith('_')).select()
    }

    // Method that removes a Byte Order Mark (BOM) from the beginning of file if it exists
    static void removeBOM(Path path){
        byte[] bytes = Files.readAllBytes(path)
        String content = new String(bytes, StandardCharsets.UTF_8)
        if (content.startsWith('\uFEFF')) {
            content = content.substring(1)
        }
        Files.write(path, content.getBytes())
    }

}
