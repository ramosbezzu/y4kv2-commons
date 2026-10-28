package net.y8k.progression;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.y8k.progression.domain.*;
import net.y8k.progression.application.*;
import static org.assertj.core.api.Assertions.*;
class SpawnDomainTest {
 @TempDir Path directory;
 @Test void gridIsBoundedSpacedAndDeterministicWithoutCentralSpawn(){
  var grid=new SpawnGrid(50,250);var uuid=UUID.randomUUID();var points=grid.candidates(10,-20,uuid);
  assertThat(points).hasSize(80).doesNotHaveDuplicates();assertThat(points).isEqualTo(grid.candidates(10,-20,uuid));
  assertThat(points).doesNotContain(new SpawnGrid.Point(10,-20));
  for(var p:points){assertThat((p.x()-10)%50).isZero();assertThat((p.z()+20)%50).isZero();assertThat((long)(p.x()-10)*(p.x()-10)+(long)(p.z()+20)*(p.z()+20)).isLessThanOrEqualTo(250L*250);}
  assertThat(grid.candidates(10,-20,UUID.randomUUID())).containsExactlyInAnyOrderElementsOf(points);
 }
 @Test void invalidBudgetsAndCoordinateOverflowFail(){
  assertThatThrownBy(()->new SpawnGrid(10,250)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->new SpawnGrid(50,2000)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->new SpawnGrid(50,251)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->new SpawnGrid(50,250).candidates(Integer.MAX_VALUE,0,UUID.randomUUID())).isInstanceOf(ArithmeticException.class);
 }
 @Test void searchSkipsReservationsAndProbesOncePerStepUntilSafe(){
  var p1=new SpawnGrid.Point(50,0);var p2=new SpawnGrid.Point(100,0);var p3=new SpawnGrid.Point(150,0);var probed=new ArrayList<SpawnGrid.Point>();
  var search=new SpawnSearch(List.of(p1,p2,p3),Set.of(p1.key()),p->{probed.add(p);return p.equals(p3)?Optional.of(70):Optional.empty();},3);
  assertThat(search.step()).isEmpty();assertThat(probed).containsExactly(p2);
  assertThat(search.step()).contains(new SpawnSearch.Destination(p3,70));assertThat(probed).containsExactly(p2,p3);
 }
 @Test void exhaustedSearchNeverProbesExtraAreas(){
  var p=new SpawnGrid.Point(50,0);var search=new SpawnSearch(List.of(p,new SpawnGrid.Point(100,0)),Set.of(),ignored->Optional.empty(),1);
  assertThat(search.step()).isEmpty();assertThat(search.exhausted()).isTrue();assertThat(search.step()).isEmpty();
  assertThatThrownBy(()->new SpawnSearch(List.of(),Set.of(),ignored->Optional.empty(),0)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void ledgerRetainsReservationsAndNearbyModifiedTerrainAfterRestart() throws Exception {
  var path=directory.resolve("data/history.txt");var world=UUID.randomUUID();var ledger=new SpawnLedger(path,world);var point=new SpawnGrid.Point(-50,0);
  ledger.reserve(UUID.randomUUID(),point);ledger.touch(-4,0);ledger.touch(-4,0);
  var reloaded=new SpawnLedger(path,world);assertThat(reloaded.used()).containsExactly(point.key());assertThat(reloaded.touched(-50,0,32)).isTrue();assertThat(reloaded.touched(200,200,32)).isFalse();
  assertThatThrownBy(()->reloaded.reserve(UUID.randomUUID(),point)).isInstanceOf(IllegalStateException.class);
  assertThat(Files.readAllLines(path)).hasSize(3);
 }
 @Test void corruptedHistoryAndWorldReplacementFailClosed() throws Exception {
  var path=directory.resolve("history.txt");var world=UUID.randomUUID();new SpawnLedger(path,world);
  assertThatThrownBy(()->new SpawnLedger(path,UUID.randomUUID())).isInstanceOf(java.io.IOException.class);
  Files.writeString(path,"world "+world+"\ndirty unsafe 0\n");assertThatThrownBy(()->new SpawnLedger(path,world)).isInstanceOf(java.io.IOException.class);
 }
 @Test void tracksRemainSeparateFromPurchasedOrStaffGroups(){
  assertThat(RankTrack.highest("survival",Set.of("owner","sigma","surv_pioneiro","sky_mestre")).name()).isEqualTo("Pioneiro");
  assertThat(RankTrack.highest("skyblock",Set.of("surv_mestre","sky_colono")).name()).isEqualTo("Colono");
  assertThat(RankTrack.highest("survival",Set.of("sigma"))).isEqualTo(RankTrack.SURVIVAL.getFirst());
  assertThatThrownBy(()->RankTrack.in("lobby")).isInstanceOf(IllegalArgumentException.class);
 }
}
