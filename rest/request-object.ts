/**
 * Some services accept this object. It abstracts the sorting and pagination as well as common attribute filters
 * You only need to set the values your request needs, everything else will use the default values
 * Endpoints using this includes Node.searchNodes, Node.getChildren or Archive.search
 */

export interface RequestObject{
  sortBy : string[];
  sortAscending : boolean[];
  offset : number;
  count : number;
  propertyFilter : string[];
}
