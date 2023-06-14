package utils;

import com.couchbase.client.java.manager.query.QueryIndex;

import java.util.stream.Collectors;

public class IndexUtils {

    public static String getIndexDefinition(QueryIndex qi) {
        StringBuilder sb = new StringBuilder();

        if (qi.primary()) {
            sb.append("CREATE PRIMARY INDEX");
        } else {
            sb.append("CREATE INDEX");
        }

        sb.append(" `").append(qi.name()).append("`");
        sb.append(" ON ");

        sb.append("`").append(qi.bucketName()).append("`");

        if (qi.scopeName().isPresent()) {
            sb.append(".`").append(qi.scopeName().get()).append("`.`")
                    .append(qi.collectionName().get()).append("`");
        }

        if (!qi.indexKey().isEmpty()) {
            sb.append("(");
            sb.append(qi.indexKey().toList().stream().map(e -> e.toString()).collect(Collectors.joining(", ")));
            sb.append(")");
        }

        if (qi.condition().isPresent()) {
            sb.append(" WHERE ").append(qi.condition().get());
        }

        if (qi.partition().isPresent()) {
            sb.append(" PARTITION BY ").append(qi.partition().get());
        }
        return sb.toString();
    }
}
