function(doc) {
    if(doc["type"] == "parameters") {
        emit([doc["problem"], doc["name"]], doc);
    }
}