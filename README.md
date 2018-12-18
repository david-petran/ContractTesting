# ContractTesting
Validate an API response matches a file loaded swagger.

## Licence
https://rem.mit-license.org/

## Examples
This example shows a test validating swagger, loaded from file, against an api response.
```
    ContractParser contract = new ContractParser("file/location/OpenAPI.yaml");
    String response = getApiResponse();
  
    String endpoint = "/path/to/endpoint";
    String resultCode = "200";
 
    ContractComparator comapartor = new ContractComparator(response, contract.getRequiredComparisonArray(endpoint, resultCode));
  
    Assert.assertTrue(comapartor.isJsonValid());
```
This example shows logging the defects in the api response
```
    if(!failingComapartor.isJsonValid()){
       for(Iterator<String> dif = failingComapartor.getDifferences().iterator(); dif.hasNext();){
           LOGGER.log( Level.SEVERE,dif.next());
       }
    }
```

