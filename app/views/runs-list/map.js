function(doc) {
    if(doc["type"] == "run") {
        emit(doc["time"], doc);
    }
}