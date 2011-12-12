================================
Asakusa Framework スタートガイド
================================
この文書では、Asakusa Frameworkをはじめて利用するユーザ向けに、Asakusa Frameworkの開発環境を作成し、サンプルアプリケーションを動かすまでの手順を説明します。

開発環境の構築
==============
Asakusa FrameworkはLinux OS上に開発環境を構築して利用します。WindowsPC上でAsakusa Frameworkを使った開発を行う場合、Windows上でLinuxの仮想マシンを実行し、ここで開発を行うと便利です。

このスタートガイドでは仮想マシンの実行ソフトウェアとして VMWare Player 、仮想マシンに使用するOSとして Ubuntu 11.10 Desktop (日本語 Remix CD x86用) を使用し、この環境に必要なソフトウェアをセットアップする手順を説明します。

..  note::
    2011年12月時点では、公式にWindows対応されているHadoopディストリビューションは存在しないため、Asakusa FrameworkもWindows上での開発をサポートしていません。将来HadoopがWindowsをサポートした場合、Asakusa FrameworkもWindows上での開発に対応する可能性があります。

VMWare Playerのインストール
---------------------------
VMWare Playerをダウンロードし、インストールを行います。

VMWare Playerのダウンロードサイト [#]_ からVMWare Player (Windows用) をダウンロードします。

ダウンロードしたインストーラを実行し、インストール画面の指示に従ってVMWare Playerをインストールします。

..  [#] http://www.vmware.com/go/get-player-jp

Ubuntu Desktop のインストール
-----------------------------
Ubuntu Desktopをダウンロードし、インストールを行います。

Ubuntu Desktop 日本語 Remix CDのダウンロードサイト [#]_ からisoファイル(CDイメージ)をダウンロードします。

..  [#] http://www.ubuntulinux.jp/products/JA-Localized/download 

ダウンロードが完了したらVMWare Playerを起動し、以下の手順に従ってUbuntu Desktopをインストールします。

1. メニューから「新規仮想マシンの作成」を選択します、
2. インストール元の選択画面では「後でOSをインストール」を選択し [#]_ 、次へ進みます。
3. ゲストOSの選択で「Linux」を選択し、バージョンに「Ubuntu」を選択して次へ進みます。
4. 仮想マシン名の入力では、任意の仮想マシン名と保存場所を指定して、次へ進みます。
5. ディスク容量の指定は任意です。デフォルトの「20GB」はAsakusa Frameworkの開発を試すには十分な容量です。お使いの環境に合わせて設定し、次へ進みます。
6. 仮想マシン作成準備画面で「ハードウェアをカスタマイズ」を選択します。デバイス一覧から「新規 CD/DVD(IDE)」を選択後、画面右側の「ISOイメージファイルを使用する」を選択し、参照ボタンを押下してダウンロードしたUbuntu Desktopのisoファイルを選択します。その他の設定は環境に合わせて設定してください。設定が完了したら画面下の閉じるボタンを押します。
7. 完了ボタンを押して仮想マシンを作成後、仮想マシンを起動すると、Ubuntu Desktopのインストールが開始します。インストール画面の指示に従ってUbuntu Desktopをインストールします。

..  [#] ここで「インストーラ ディスク イメージ ファイル」を選択し、isoファイルを選択するとOSの「簡易インストール」が行われますが、簡易インストールでは日本語環境がインストールされないほか、いくつかの設定が適切に行われないため、簡易インストールの使用は推奨しません。

Ubuntu Desktopが起動したら、同梱のブラウザなどを使用してUbuntuからインターネットにできることを確認してください。以後の手順ではインターネットに接続できることを前提とします。

また、以降の手順で使用するホームフォルダ直下のダウンロードディレクトリを日本語名から英語に変更するため、ターミナルを開いて以下のコマンドを実行します。

..  code-block:: sh

    LANG=C xdg-user-dirs-gtk-update

ダイアログが開いたら「次回からチェックしない」にチェックを入れ、「Update Names」を選択します。

そのほか、必須の手順ではないですがここでVMWare Toolsをインストールしておくとよいでしょう。

Java(JDK)のインストール
-----------------------
Hadoop、及びAsakusa Frameworkの実行に使用するJavaをインストールします。

Javaのダウンロードサイト [#]_ から、Java SE 6 の JDK をダウンロードします [#]_ 。

ダウンロードが完了したら、以下の例を参考にしてJavaをインストールします。

..  code-block:: sh

    cd ~/Downloads
    chmod +x jdk*
    ./jdk*
    
    sudo mkdir /usr/lib/jvm
    sudo mv jdk1.6.0_* /usr/lib/jvm

    cd /usr/lib/jvm
    sudo ln -s jdk1.6.0_* jdk-6

..  [#] http://www.oracle.com/technetwork/java/javase/downloads/index.html
..  [#] ダウンロードするファイルは「jdk-6uXX-linux-i586.bin」(XXはUpdate番号) です。本スタートガイドの環境に従う場合は、x64版(xx-ia64.bin)や、RPM版のファイル(xx-rpm.bin)をダウンロードしないよう注意してください。

このほかに環境変数の設定が必要ですが、本手順では後ほどまとめて設定するため、このまま次に進みます。

Mavenのインストール
-------------------
Asakusa Frameworkの開発環境に必要なビルドツールであるMavenをインストールします。

Mavenのダウンロードサイト [#]_ から Maven3 のtarball [#]_ をダウンロードします。

ダウンロードが完了したら、以下の例を参考にしてMavenをインストールします。

..  code-block:: sh

    cd ~/Downloads
    tar xf apache-maven-*-bin.tar.gz
    sudo mv apache-maven-* /usr/local/lib
    ln -s /usr/local/lib/apache-maven-*/bin/mvn /usr/local/bin/mvn

..  [#] http://maven.apache.org/download.html
..  [#] apache-maven-3.X.X-bin.tar.gz

..  note:: 
    インターネットへの接続にプロキシサーバを経由する必要がある環境については、Mavenに対してプロキシの設定を行う必要があります。設定についてはMavenの次のサイト等を確認してください

    http://maven.apache.org/guides/mini/guide-proxies.html

Hadoopのインストール
--------------------
Clouderaから提供されているHadoopのディストリビューションである Cloudera Hadoop Distribution of Hadoop version 3(CDH3)をインストールします。

CDH3のインストール方法はOS毎に提供されているインストールパッケージを使う方法と、tarballを展開する方法がありますが、ここではtarballを展開する方法でインストールします。

CDH3のダウンロードサイト [#]_ から CDH3 のtarball [#]_ をダウンロードします。コンポーネントはHadoopのみをダウンロードします。

..  [#] https://ccp.cloudera.com/display/SUPPORT/CDH3+Downloadable+Tarballs
..  [#] hadoop-0.20.2-cdh3uX.tar.gz

ダウンロードが完了したら、以下の例を参考にしてCDH3をインストールします。

..  code-block:: sh

    cd ~/Downloads
    tar xf hadoop-0.20.2-*.tar.gz
    mv hadoop-0.20.2-*.tar.gz /tmp
    sudo mv hadoop-0.20.2-* /usr/lib
    sudo ln -s /usr/lib/hadoop-0.20.2-* /usr/lib/hadoop

環境変数の設定
--------------
Asakusa Frameworkの利用に必要となる環境変数を設定します。

$HOME/.profile の最下行に以下の定義を追加します。

..  code-block:: sh

    export JAVA_HOME=/usr/lib/jvm/jdk-6
    export HADOOP_HOME=/usr/lib/hadoop
    export ASAKUSA_HOME=$HOME/asakusa
    export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$PATH

環境変数をデスクトップ環境に反映させるため、一度デスクトップ環境からログアウトし、再ログインします。

Eclipseのインストール
---------------------
アプリケーションの実装・テストに使用する統合開発環境(IDE)として、Eclipseをインストールします。

..  note:: Asakusa Frameworkを使う上でEclipseの使用は必須ではありません。サンプルアプリケーションのソースを確認する場合などでEclipseがあると便利であると思われるため、ここでEclipseのインストールを説明していますが、スタートガイドの手順のみを実行するのであれば、Eclipseのインストールは不要です。

Eclipseのダウンロードサイト [#]_ から Eclipse IDE for Java Developers (Linux 32 Bit) [#]_ をダウンロードします。

ダウンロードが完了したら、以下の例を参考にしてEclipseをインストールします。

..  code-block:: sh

    cd ~/Downloads
    tar xf eclipse-java-*-linux-gtk.tar.gz
    sudo mv eclipse /usr/local/lib

次に、Eclipseのワークスペースに対してクラスパス変数M2_REPOを設定します。ここでは、ワークスペースディレクトリに$HOME/workspace を指定します。

..  code-block:: sh

    mvn -Declipse.workspace=$HOME/workspace eclipse:add-maven-repo

Eclipseを起動するには、ファイラーから /usr/local/lib/eclipse/eclipse を実行します。ワークスペースはデフォルトの$HOME/workspace をそのまま指定します。

Eclipseの起動が完了したら、以降の手順を実行するために、ターミナルのカレントディレクトリをワークスペースのディレクトリに移動しておきましょう。

..  code-block:: sh

    cd $HOME/workspace

..  [#] http://www.eclipse.org/downloads/
..  [#] eclipse-java-XX-linux-gtk.tar.gz

Asakusa Frameworkのインストールとサンプルアプリケーションの実行
===============================================================
開発環境にAsakusa Frameworkをインストールして、Asakusa Frameworkのサンプルアプリケーションを実行してみます。

アプリケーション開発プロジェクトの作成
--------------------------------------
まず、Asakusa Frameworkのバッチアプリケーションを開発、及び管理する単位となる「プロジェクト」を作成します。

Asakusa Frameworkでは、プロジェクトのテンプレートを提供しており、このテンプレートにサンプルアプリケーションも含まれています。また、このテンプレートに含まれるスクリプトを使ってAsakusa Frameworkを開発環境にインストールすることができます。

プロジェクトのテンプレートはMavenのアーキタイプという仕組みで提供されています。Mavenのアーキタイプからプロジェクトを作成するには、以下のコマンドを実行します。

..  code-block:: sh

    mvn archetype:generate -DarchetypeCatalog=http://asakusafw.s3.amazonaws.com/maven/archetype-catalog.xml

コマンドを実行すると、Asakusa Frameworkが提供するプロジェクトテンプレートのうち、どれを使用するかを選択する画面が表示されます。ここでは、3 (asakusa-archetype-windgate) のWindGateと連携するアプリケーション用のテンプレートを選択します。

..  code-block:: sh

    1: http://asakusafw.s3.amazonaws.com/maven/archetype-catalog.xml -> com.asakusafw:asakusa-archetype-batchapp (-)
    2: http://asakusafw.s3.amazonaws.com/maven/archetype-catalog.xml -> com.asakusafw:asakusa-archetype-thundergate (-)
    3: http://asakusafw.s3.amazonaws.com/maven/archetype-catalog.xml -> com.asakusafw:asakusa-archetype-windgate (-)
    Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): : 3 (<-3を入力)

次に、Asakusa Frameworkのバージョンを選択します。ここでは 4 (0.2.4) を選択します。

..  code-block:: sh

    Choose com.asakusafw:asakusa-archetype-windgate version: 
    1: 0.2-SNAPSHOT
    2: 0.2.2
    3: 0.2.3
    4: 0.2.4
    Choose a number: 4: 4 (<-4を入力)

この後、アプリケーションプロジェクトに関するいくつかの定義を入力します。いずれも任意の値を入力することが出来ます。ここでは、アプリケーションプロジェクト名として「sample-app」を指定します。最後に確認をうながされるので、そのままEnterキーを入力します。

..  code-block:: sh

    Define value for property 'groupId': : com.example [<-アプリケーションのグループ名]
    Define value for property 'artifactId': : sample-app [<-アプリケーションのプロジェクト名]
    Define value for property 'version':  1.0-SNAPSHOT [<-アプリケーションのバージョン]
    Define value for property 'package':  com.example [<-アプリケーションの基底パッケージ名]

    Confirm properties configuration:
    groupId: com.example
    artifactId: sample-app
    version: 1.0-SNAPSHOT
    package: com.example
    Y: : [<-そのままEnterキーを入力]

入力が終わるとプロジェクトの作成が始まります。成功した場合、画面に以下のように「BUILD SUCCESS」と表示されます。

..  code-block:: sh

    ...
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 20.245s
    ...

..  note::
    以降の手順についても、Mavenのコマンド実行後に処理が成功したかを確認するには「BUILD SUCCESS」が表示されていることを確認してください。

これでアプリケーションプロジェクトが作成されました。以降の手順を実行するため、作成されたプロジェクトに移動しておきましょう。

..  code-block:: sh

    cd sample-app

Asakusa Frameworkのインストール
-------------------------------
Asakusa Frameworkを開発環境にインストールします。

先ほど作成したアプリケーションプロジェクトから、Mavenの以下のコマンドを使ってAsakusa Frameworkをローカルにインストールすることができます。

..  code-block:: sh

    mvn assembly:single antrun:run

成功すると、$ASAKUSA_HOME (このスタートガイドでは $HOME/asakusa) にAsakusa Frameworkがインストールされます。

サンプルアプリケーションのビルド
--------------------------------
アプリケーションのテンプレートには、あらかじめサンプルアプリケーション（カテゴリー別売上金額集計バッチ) のソースファイルが含まれています。このサンプルアプリケーションのソースファイルをAsakusa Framework上で実行可能な形式にビルドします。

アプリケーションのビルドを実行するには、Mavenの以下のコマンドを実行します。

..  code-block:: sh

    mvn package

このコマンドの実行によって、サンプルアプリケーションに対して以下の処理が実行されます。

1. データモデル定義DSL(DMDL)から、データモデルクラスを生成
2. Asakusa DSLとデータモデル定義DSLから、実行可能なプログラム群（HadoopのMapReduceジョブやWindGate用の実行定義ファイルなど)を生成
3. 実行可能なプログラム群に対するテストを実行
4. サンプルアプリケーションを運用環境に配置するためのアーカイブファイルを生成

ビルドが成功すると、プロジェクトのtargetディレクトリ配下にいくつかのファイルが作成されますが、この中の 「sample-app-batchapps-1.0-SNAPSHOT.jar」 というファイルがサンプルアプリケーションが含まれるアーカイブファイルです。

..  note::
    このアーカイブファイルの名前は、実際には ${artifactId}-batchapp-${version}.jar という命名ルールに従って作成されます。プロジェクト作成時に本ドキュメントの例以外のプロジェクト名やバージョンを指定した場合は、それに合わせて読み替えてください。
    
..  warning::
    targetディレクトリの配下に似た名前のファイルとして ${artifactId}-${version}.jar というファイル(「batchapp」が付いていないjarファイル)が同時に作成されますが、これは実行可能なアーカイブファイルではないので注意してください。

サンプルアプリケーションのデプロイ
----------------------------------
サンプルアプリケーションを実行するために、先ほどビルドしたサンプルアプリケーションを実行環境にデプロイします。

実行環境は、通常はHadoopクラスターが構築されている運用環境となりますが、ここでは開発環境（ローカル）上のHadoopとAsakusa Framework上でサンプルアプリケーションを実行するため、ローカルに対するデプロイを行います。

アプリケーションのデプロイは、Asakusa Frameworkがインストールされているマシン上の $ASAKUSA_HOME/batchapps ディレクトリに アプリケーションが含まれるjarファイルの中身を展開して配置します。以下はアプリケーションプロジェクトで生成したアーカイブファイルをローカルのAsakusa Frameworkにデプロイする例です。

..  code-block:: sh

    cp target/*batchapps*.jar $ASAKUSA_HOME/batchapps
    cd $ASAKUSA_HOME/batchapps
    jar xf *batchapps*.jar

サンプルデータの作成と配置
--------------------------
カテゴリー別売上金額集計バッチは、売上トランザクションデータと、商品マスタ、店舗マスタを入力として、エラーチェックを行った後、商品マスタのカテゴリ毎に集計するアプリケーションです。入力データの取得と出力データの生成はそれぞれCSVファイルに対して行うようになっています。

このバッチは入力データを /tmp/windgate-$USER ($USERはOSユーザ名に置き換え) ディレクトリから取得するようになっています。プロジェクトにはあらかじめ src/test/example-dataset ディレクトリ以下にテストデータが用意されているので、これらのファイルを  /tmp/windgate-$USER 配下にコピーします。

..  code-block:: sh

   cd $HOME/workspace/sample-app
   cp -r src/test/example-dataset/* /tmp/windgate-$USER

サンプルアプリケーションの実行
------------------------------
ローカルにデプロイしたサンプルアプリケーションを実行します。

Asakusa Frameworkでは、バッチアプリケーションを実行するためのコマンドプログラムとして「YAESS」というツールが提供されています。バッチアプリケーションを実行するには、 $ASAKUSA_HOME/yaess/bin/yaess-batch.sh に実行するバッチのバッチIDを指定します。

サンプルアプリケーション「カテゴリー別売上金額集計バッチ」のバッチIDは「example.summarizeSales」というIDを持っています。また、このバッチは引数に処理対象の売上日時(date)を指定し、この値に基づいて処理対象CSVファイルを特定します。

バッチIDとバッチ引数を指定して、以下のようにバッチアプリケーションを実行します。

..  code-block:: sh

    $ASAKUSA_HOME/yaess/bin/yaess-batch.sh example.summarizeSales -A date=2011-01-01

バッチの実行が成功すると、コマンドの標準出力の最終行に「Finished: SUCCESS」と出力されます。

..  code-block:: sh

    ...
    2011/12/08 16:54:38 INFO  [JobflowExecutor-example.summarizeSales] END PHASE - example.summarizeSales|byCategory|CLEANUP@cc5c8cfd-604b-4652-a387-b2ea4d463943
    2011/12/08 16:54:38 DEBUG [JobflowExecutor-example.summarizeSales] Completing jobflow "byCategory": example.summarizeSales
    Finished: SUCCESS

カテゴリー別売上金額集計バッチはバッチの実行結果として、ディレクトリ /tmp/windgate-$USER/result に集計データがCSVファイルとして出力されます。CSVファイルの中身を確認すると、売上データがカテゴリー毎に集計されている状態で出力されています。

Eclipseへアプリケーションプロジェクトをインポート
-------------------------------------------------
アプリケーションプロジェクトをEclipseへインポートして、Eclipse上でアプリケーションの開発を行えるようにします。

インポートするプロジェクトのディレクトリに移動し、Mavenの以下のコマンドを実行してEclipse用の定義ファイルを作成します。

..  code-block:: sh

    cd $HOME/workspace/sample-app
    mvn eclipse:eclipse

これでEclipseからプロジェクトをImport出来る状態になりました。Eclipseのメニューから [File] -> [Import] -> [General] -> [Existing Projects into Workspace] を選択し、プロジェクトディレクトリを指定してEclipseにインポートします。

Next Step:アプリケーションの開発を行う
======================================
本スタートガイドの手順を実行し、Asakusa Framework上でバッチアプリケーションの開発を行う準備が出来ました。

次に、アプリケーションの開発を行うために、Asakusa Frameworkを使ったアプリケーション開発の流れを見てみましょう。 >> :doc:`next-step`
