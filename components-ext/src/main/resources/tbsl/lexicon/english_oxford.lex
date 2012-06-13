
// PREPOSITIONS

  close to  || (NP NP* (PP P:'close' P:'to' DP[dp])) || <x,l1,<e,t>, [ l1:[ | SLOT_closeto(x,y) ] ], [ (l2,y,dp,<<e,t>,t>) ], [ l2=l1 ],[ SLOT_closeto/OBJECTPROPERTY/near ]>
  in        || (NP NP* (PP P:'in' DP[dp]))           || <x,l1,<e,t>, [ l1:[ | SLOT_location(x,y) ] ], [ (l2,y,dp,<<e,t>,t>) ], [ l2=l1 ],[ SLOT_location/PROPERTY/location^city^postal_code^address ]>
  since     || (NP NP* (PP P:'since' DP[dp]))        || <x,l1,<e,t>, [ l1:[ | SLOT_since(x,y) ] ], [ (l2,y,dp,<<e,t>,t>) ], [ l2=l1 ],[ SLOT_since/PROPERTY/since ]>
  
  for . pounds || (NP NP* (PP P:'for' (NP NUM[num] N:'pounds'))) || <x,l1,<e,t>, [ l1:[ | SLOT_price(x,y) ] ], [ (l2,y,num,e) ], [ l2=l1 ],[ SLOT_price/DATATYPEPROPERTY/price ]>
  for more than . pounds || (NP NP* (PP P:'for' (NP NUM[num] N:'pounds'))) || <x,l1,<e,t>, [ l1:[ | SLOT_price(x,y), greater(y,z) ] ], [ (l2,z,num,e) ], [ l2=l1 ],[ SLOT_price/DATATYPEPROPERTY/price ]>
  for less than . pounds || (NP NP* (PP P:'for' (NP NUM[num] N:'pounds'))) || <x,l1,<e,t>, [ l1:[ | SLOT_price(x,y), less(y,z) ] ], [ (l2,y,num,e) ], [ l2=l1 ],[ SLOT_price/DATATYPEPROPERTY/price ]>
  from . to . pounds || (NP NP* (PP P:'from' NUM[num1] P:'to' NUM[num2] N:'pounds')) || <x,l1,<e,t>, [ l1:[ | SLOT_price(x,y), greaterorequal(y,n1), lessorequal(y,n2) ] ], [ (l2,n1,num1,e),(l3,n2,num2,e) ], [ l2=l1,l3=l1 ],[ SLOT_price/DATATYPEPROPERTY/price ]>

  with || (NP NP* (PP P:'with' DP[dp])) || <x,l1,<e,t>, [ l1:[ | empty(x,y) ] ], [ (l2,y,dp,<<e,t>,t>) ], [ l2=l1 ],[]>
  square meters || (DP N:'square' N:'meters') || <x,l1,<<e,t>,t>>, [l1:[ | SLOT_size(x,y) ]], [],[],[SLOT_size/DATATYPEPROPERTY/size ]>

// MONTHS 

  january   || (DP DP:'january') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,1) ]], [],[],[]>
  february  || (DP DP:'february') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,2) ]], [],[],[]>
  march     || (DP DP:'march') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,3) ]], [],[],[]>
  april     || (DP DP:'april') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,4) ]], [],[],[]>
  may       || (DP DP:'may') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,5) ]], [],[],[]>
  june      || (DP DP:'june') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,6) ]], [],[],[]>
  july      || (DP DP:'july') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,7) ]], [],[],[]>
  august    || (DP DP:'august') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,8) ]], [],[],[]>
  september || (DP DP:'september') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,9) ]], [],[],[]>
  october   || (DP DP:'october') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,10) ]], [],[],[]>
  november  || (DP DP:'november') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,11) ]], [],[],[]>
  december  || (DP DP:'december') || <x,l1,<<e,t>,t>, [ l1:[ x | xsd:month(x,12) ]], [],[],[]>