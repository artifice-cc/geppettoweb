function(doc) {
    if(doc["type"] == "claim") {
        emit(doc["created"], doc);
    }
}
