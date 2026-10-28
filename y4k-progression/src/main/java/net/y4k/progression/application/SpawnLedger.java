package net.y4k.progression.application;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import net.y4k.progression.domain.SpawnGrid.Point;
/** Private append-only history. Used cells and touched chunks stay excluded after restart. */
public final class SpawnLedger {
 private final Path path;
 private final Set<String> used=new HashSet<>(),dirty=new HashSet<>();
 public SpawnLedger(Path path,UUID world) throws IOException {
  this.path=path; Files.createDirectories(path.getParent());
  if(!Files.exists(path)) Files.writeString(path,"world "+world+"\n",StandardOpenOption.CREATE_NEW);
  try { Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rw-------")); } catch(UnsupportedOperationException ignored) { /* platform ACL applies */ }
  var lines=Files.readAllLines(path);
  if(lines.isEmpty() || !lines.getFirst().equals("world "+world)) throw new IOException("Spawn ledger world mismatch");
  for(var line:lines.subList(1,lines.size())) {
   var parts=line.split(" ");
   try {
    if(parts.length==3 && parts[0].equals("dirty")) dirty.add(point(parts[1],parts[2]).key());
    else if(parts.length==4 && parts[0].equals("used")) { UUID.fromString(parts[1]); used.add(point(parts[2],parts[3]).key()); }
    else throw new IllegalArgumentException();
   } catch(IllegalArgumentException e) { throw new IOException("Invalid spawn ledger; allocation refused",e); }
  }
 }
 private Point point(String x,String z) { return new Point(Integer.parseInt(x),Integer.parseInt(z)); }
 public Set<String> used() { return Set.copyOf(used); }
 public void reserve(UUID player,Point point) throws IOException {
  if(used.contains(point.key())) throw new IllegalStateException("Spawn cell already reserved");
  Files.writeString(path,"used "+player+" "+point.x()+" "+point.z()+"\n",StandardOpenOption.APPEND);used.add(point.key());
 }
 public void touch(int x,int z) throws IOException {
  var point=new Point(x,z);if(dirty.contains(point.key()))return;
  Files.writeString(path,"dirty "+x+" "+z+"\n",StandardOpenOption.APPEND);dirty.add(point.key());
 }
 public boolean touched(int x,int z,int clearance) {
  for(int cx=(x-clearance)>>4;cx<=(x+clearance)>>4;cx++)for(int cz=(z-clearance)>>4;cz<=(z+clearance)>>4;cz++)
   if(dirty.contains(new Point(cx,cz).key()))return true;
  return false;
 }
}
