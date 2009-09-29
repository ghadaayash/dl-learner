<?php
session_start();

require('../classes/Config.php'); 
require('../classes/DllearnerConnection.php');

$c = new DllearnerConnection();

// Instances are the positive Examples mixed up with some
// other examples, thus random records
$instances = array();
$numberOfInstaces = 10;

// some stoner records
$posExamples = array(
  "http://dbtune.org/jamendo/record/1128",
  "http://dbtune.org/jamendo/record/8620",
  "http://dbtune.org/jamendo/record/8654",
  "http://dbtune.org/jamendo/record/10031"
);

// take the last 1/3 of total as positive examples
$howManyPosExamples = round($numberOfInstaces/3);
$totalPosExamples = count($posExamples);
for ($i = $totalPosExamples; $i > ($totalPosExamples - $howManyPosExamples); $i--) {
  $instances[] = $posExamples[$i-1];
}
// and then add some random Records _not_ in this list
$allRecords = file($c->getConfigUrl('allRecords'));
$howManyRandomRecords = $numberOfInstaces - $howManyPosExamples;
for ($i = 0; $i < $howManyRandomRecords; $i++) {
  // cutting of linebreak
  $randomRecord = trim($allRecords[array_rand($allRecords)]);
  if (!in_array($randomRecord, $instances)) {
    $instances[] = $randomRecord;
  }
  
}

$instances = array(  
  "http://dbtune.org/jamendo/record/1128",
  "http://dbtune.org/jamendo/record/8620",
  "http://dbtune.org/jamendo/record/8654",
  "http://dbtune.org/jamendo/record/10031",
  "http://dbtune.org/jamendo/record/1465",
  "http://dbtune.org/jamendo/record/1568",
  "http://dbtune.org/jamendo/record/1668",
  "http://dbtune.org/jamendo/record/1769",
  "http://dbtune.org/jamendo/record/1869",
  "http://dbtune.org/jamendo/record/1970"
);

echo '<pre>';
print_r($instances);
print_r($c->getLearningConfig());
print_r($posExamples);
print_r($c->learn($instances, $posExamples, 'file:/Users/nebelschwade/Sites/moosique.net/moosique/testing/rocksubset.owl'));

echo '</pre>';
?>