# 串行GC

### 执行命令

```
java -XX:+UseSerialGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis
```

### 内存分配

```
Heap
 def new generation   total 157248K, used 46246K [0x00000000e0000000, 0x00000000eaaa0000, 0x00000000eaaa0000)
  eden space 139776K,  33% used [0x00000000e0000000, 0x00000000e2d298c8, 0x00000000e8880000)
  from space 17472K,   0% used [0x00000000e8880000, 0x00000000e8880000, 0x00000000e9990000)
  to   space 17472K,   0% used [0x00000000e9990000, 0x00000000e9990000, 0x00000000eaaa0000)
 tenured generation   total 349568K, used 349333K [0x00000000eaaa0000, 0x0000000100000000, 0x0000000100000000)
   the space 349568K,  99% used [0x00000000eaaa0000, 0x00000000fffc5638, 0x00000000fffc5800, 0x0000000100000000)
 Metaspace       used 2710K, capacity 4486K, committed 4864K, reserved 1056768K
  class space    used 294K, capacity 386K, committed 512K, reserved 1048576K
```

年轻代中Eden、from、to所占的空间是8：1：1的比例，但eden+from=年轻代的总大小，说明jvm进行了自适应调整，而年轻代和老年代的比例大概是3:7，剩下非堆中的元空间和类结构文件共占用了不到5M的少量空间。

### GC总结

第一次youngGC

```
2021-02-13T14:56:20.574+0800: [GC (Allocation Failure) 2021-02-13T14:56:20.574+0800: [DefNew: 139562K->17472K(157248K), 0.0127731 secs] 139562K->45382K(506816K), 0.0129889 secs] [Times: user=0.00 sys=0.02, real=0.01 secs]
```

将年轻代的所有剩余对象分配到了from区，另外还有45382K-17472K的对象进入了老年代

第一次fullGC

```
[Full GC (Allocation Failure) 2021-02-13T14:56:21.117+0800: [Tenured: 349527K->348389K(349568K), 0.0347577 secs] 506307K->348389K(506816K), [Metaspace: 2703K->2703K(1056768K)], 0.0351490 secs] [Times: user=0.03 sys=0.00, real=0.03 secs]
```

将老年代的内存从349527K压缩到了348389K，老年代几乎被占满，回收几乎没有效果。

年轻代被清空，还尝试回收了metaspace，但是没有回收成功。

最后一次FullGC

```
2021-02-13T14:56:21.505+0800: [Full GC (Allocation Failure) 2021-02-13T14:56:21.505+0800: [Tenured: 349567K->349333K(349568K), 0.0240029 secs] 506806K->390029K(506816K), [Metaspace: 2703K->2703K(1056768K)], 0.0242707 secs] [Times: user=0.02 sys=0.00, real=0.02 secs]
```

之后进行FullGC，老年代基本上没法回收，而总的堆内存占用在变多，说明年轻代正在堆积老年代放不下的对象，

再持续下去就会发生OOM

# 并行GC

### 执行命令

```
java -XX:+UseParallelGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis
```

### 内存分配

```
Heap
 PSYoungGen      total 117248K, used 2737K [0x00000000f5580000, 0x0000000100000000, 0x0000000100000000)
  eden space 59392K, 4% used [0x00000000f5580000,0x00000000f582c6e0,0x00000000f8f80000)
  from space 57856K, 0% used [0x00000000fc780000,0x00000000fc780000,0x0000000100000000)
  to   space 57344K, 0% used [0x00000000f8f80000,0x00000000f8f80000,0x00000000fc780000)
 ParOldGen       total 349696K, used 348450K [0x00000000e0000000, 0x00000000f5580000, 0x00000000f5580000)
  object space 349696K, 99% used [0x00000000e0000000,0x00000000f54489b0,0x00000000f5580000)
 Metaspace       used 2710K, capacity 4486K, committed 4864K, reserved 1056768K
  class space    used 294K, capacity 386K, committed 512K, reserved 1048576K
```

同样开启了自适应，年轻代三个分区约为1：1：1，eden+from=年轻代的总容量，年轻代的标称容量和GC时的标称容量不相符。

### GC总结

第一次youngGC

```
2021-02-13T15:20:07.081+0800: [GC (Allocation Failure) [PSYoungGen: 131584K->21481K(153088K)] 131584K->38905K(502784K), 0.0051202 secs] [Times: user=0.13 sys=0.08, real=0.00 secs]
```

把young区的总容量从131584K压缩到了21481K，进入了from区，另有38905K-21481K的对象进入到了老年代，此次youngGC耗时0.0051202秒

第一次fullGC

```
2021-02-13T15:20:07.459+0800: [Full GC (Ergonomics) [PSYoungGen: 20308K->0K(116736K)] [ParOldGen: 320207K->313816K(349696K)] 340516K->313816K(466432K), [Metaspace: 2703K->2703K(1056768K)], 0.0290169 secs] [Times: user=0.41 sys=0.00, real=0.03 secs]
```

清空了young区，把老年代从320207K压缩到了313816K，几乎没清理多少对象

最后一次fullGC

```
2021-02-13T15:20:08.035+0800: [Full GC (Ergonomics) [PSYoungGen: 58880K->0K(116736K)] [ParOldGen: 346794K->346799K(349696K)] 405674K->346799K(466432K), [Metaspace: 2703K->2703K(1056768K)], 0.0315097 secs] [Times: user=0.22 sys=0.00, real=0.03 secs]
```

老年代动弹不得，而总的内存从405674K压缩到了346799K，看来是清空了young区

如果时间再长点，就要OOM了

# CMS

### 执行命令

```
java -XX:+UseConcMarkSweepGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis
```

### 内存分配

```
Heap
 par new generation   total 157248K, used 21339K [0x00000000e0000000, 0x00000000eaaa0000, 0x00000000eaaa0000)
  eden space 139776K,  15% used [0x00000000e0000000, 0x00000000e14d6cd8, 0x00000000e8880000)
  from space 17472K,   0% used [0x00000000e9990000, 0x00000000e9990000, 0x00000000eaaa0000)
  to   space 17472K,   0% used [0x00000000e8880000, 0x00000000e8880000, 0x00000000e9990000)
 concurrent mark-sweep generation total 349568K, used 349139K [0x00000000eaaa0000, 0x0000000100000000, 0x0000000100000000)
 Metaspace       used 2710K, capacity 4486K, committed 4864K, reserved 1056768K
  class space    used 294K, capacity 386K, committed 512K, reserved 1048576K
```

内存分布和上面串行GC时的基本一致

老年代的名称变成了**并发标记清理代**。为了方便，下文还是以老年代称呼。

### GC总结

第一次youngGC

```
2021-02-13T15:33:40.633+0800: [GC (Allocation Failure) 2021-02-13T15:33:40.633+0800: [ParNew: 139692K->17472K(157248K), 0.0056892 secs] 139692K->46410K(506816K), 0.0058200 secs] [Times: user=0.00 sys=0.00, real=0.01 secs]
```

和并行GC无异，STW，标记复制，其中有46410K-17472K进入了老年代

#### 一次FullGC过程

- 初始标记

```
2021-02-13T15:33:41.548+0800: [GC (CMS Initial Mark) [1 CMS-initial-mark: 349219K(349568K)] 358914K(506816K), 0.0007046 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
```

有0.0082313s的短暂停，在老年代标记了216354K的**根对象**，年轻代标记了236658K-216354K的**根对象**

- 并发标记

```
2021-02-13T15:33:41.549+0800: [CMS-concurrent-mark-start]
2021-02-13T15:33:41.550+0800: [CMS-concurrent-mark: 0.001/0.001 secs] [Times: user=0.08 sys=0.00, real=0.00 secs]
```

不STW，标记老年代的所有存活对象

- 并发预清理

```
2021-02-13T15:33:41.550+0800: [CMS-concurrent-preclean-start]
2021-02-13T15:33:41.551+0800: [CMS-concurrent-preclean: 0.001/0.001 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
2021-02-13T15:33:41.551+0800: [CMS-concurrent-abortable-preclean-start]
2021-02-13T15:33:41.551+0800: [CMS-concurrent-abortable-preclean: 0.000/0.000 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
```

进行卡片标记

- 最终标记

```
2021-02-13T15:33:41.551+0800: [GC (CMS Final Remark) [YG occupancy: 41773 K (157248 K)]2021-02-13T15:33:41.551+0800: [Rescan (parallel) , 0.0002244 secs]2021-02-13T15:33:41.552+0800: [weak refs processing, 0.0000360 secs]2021-02-13T15:33:41.552+0800: [class unloading, 0.0001971 secs]2021-02-13T15:33:41.552+0800: [scrub symbol table, 0.0002656 secs]2021-02-13T15:33:41.552+0800: [scrub string table, 0.0001027 secs][1 CMS-remark: 349219K(349568K)] 390993K(506816K), 0.0009701 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
```

STW,完成老年代所有存活对象的标记，为并发清除做准备

- 并发清除

```
2021-02-13T15:33:41.552+0800: [CMS-concurrent-sweep-start]
2021-02-13T15:33:41.553+0800: [CMS-concurrent-sweep: 0.000/0.000 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
2021-02-13T15:33:41.553+0800: [CMS-concurrent-reset-start]
```

- 并发重置

```
2021-02-13T15:33:41.553+0800: [CMS-concurrent-reset: 0.000/0.000 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
```

# G1

### 命令

```
java -XX:+UseG1GC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps GCLogAnalysis
```

### 内存分配

```
Heap
 garbage-first heap   total 524288K, used 374746K [0x00000000e0000000, 0x00000000e0101000, 0x0000000100000000)
  region size 1024K, 11 young (11264K), 1 survivors (1024K)
 Metaspace       used 2710K, capacity 4486K, committed 4864K, reserved 1056768K
  class space    used 294K, capacity 386K, committed 512K, reserved 1048576K
```

每个区域大小1M，年轻代占了11个，survirors区域占了1个

### GC总结

与CMSGC类似，区别是每次只处理一部分内存块。