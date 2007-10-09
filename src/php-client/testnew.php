<?php

include_once("LearnerClient.php");

ini_set("soap.wsdl_cache_enabled","0");

$wsdluri="http://localhost:8181/services?wsdl";
$ontology="file:/home/jl/promotion/dl-learner-svn/trunk/examples/father.owl";

$posExamples = array('http://example.com/father#stefan',
                     'http://example.com/father#markus',
                     'http://example.com/father#martin');
$negExamples = array('http://example.com/father#heinz',
                     'http://example.com/father#anna',
                     'http://example.com/father#michelle');

// always update WSDL
LearnerClient::loadWSDLfiles($wsdluri);

// test web service
$client = new SoapClient("main.wsdl");

$id = $client->generateID();

$client->addKnowledgeSource($id, "owlfile", $ontology);
$client->setReasoner($id, "dig");
$client->setLearningProblem($id, "posNegDefinition");
$client->setPositiveExamples($id, $posExamples);
$client->setNegativeExamples($id, $negExamples);
$client->setLearningAlgorithm($id, "refinement");

$start = microtime(true);

$client->init($id);

$learn_start = microtime(true);
$init = $learn_start - $start;
echo 'components initialised in '.$init.' seconds<br />';

$concept = $client->learn($id);

$learn = microtime(true) - $learn_start;
echo 'concept learned in '.$learn.' seconds<br />';

echo 'result: '.$concept;

?>