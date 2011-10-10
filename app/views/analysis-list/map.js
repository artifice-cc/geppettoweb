function(doc) {
    if(doc["type"] == "analysis") {
        emit([doc["problem"], doc["name"]], doc)
    }
}