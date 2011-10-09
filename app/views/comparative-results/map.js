function(doc) {
    if(doc["type"] == "comparative") {
        for(var k in doc) {
            emit([doc["runid"], k], doc[k]);
        }
    }
}
