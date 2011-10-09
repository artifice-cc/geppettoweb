function(doc) {
    if(doc["type"] == "graph") {
        emit([doc["problem"], doc["name"]], doc)
    }
}