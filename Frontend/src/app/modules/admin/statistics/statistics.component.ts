import {RestAdminService} from "../../../core-module/rest/services/rest-admin.service";
import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import {TranslateService} from "@ngx-translate/core";
import {NodeStatistics, Node, Statistics} from "../../../core-module/rest/data-object";
import {ListItem} from "../../../core-module/ui/list-item";
import {RestConstants} from "../../../core-module/rest/rest-constants";
import {RestHelper} from "../../../core-module/rest/rest-helper";
import {NodeHelper} from "../../../core-ui-module/node-helper";
import {ConfigurationService} from "../../../core-module/rest/services/configuration.service";
import {UIHelper} from "../../../core-ui-module/ui-helper";
import {RestStatisticsService} from "../../../core-module/rest/services/rest-statistics.service";
import {AuthorityNamePipe} from "../../../core-ui-module/pipes/authority-name.pipe";

// Charts.js
declare var Chart:any;

@Component({
  selector: 'app-admin-statistics',
  templateUrl: 'statistics.component.html',
  styleUrls: ['statistics.component.scss']
})
export class AdminStatisticsComponent {
  @ViewChild('groupedChart') groupedChartRef : ElementRef;
  private _mediacenter:any;
  @Input() set mediacenter(mediacenter: any){
    this._mediacenter=mediacenter;
    this.refresh();
  }
  @Output() onOpenNode = new EventEmitter();
  static DAY_OFFSET=1000*60*60*24;
  static DEFAULT_OFFSET=AdminStatisticsComponent.DAY_OFFSET*7; // 7 days
  static DEFAULT_OFFSET_SINGLE=AdminStatisticsComponent.DAY_OFFSET*3; // 3 days
  today = new Date();
  _groupedStart=new Date();
  _groupedEnd=new Date();
  _singleStart=new Date();
  _singleEnd=new Date();
  _customGroupStart=new Date();
  _customGroupEnd=new Date();
  _customGroup : string;
  _customUnfold = '';
  _nodesStart=new Date();
  _nodesEnd=new Date();
  customGroupRows:string[];
  additionalGroups:string[];
  customGroups:string[];
  customGroupData:any;
  customGroupLabels:any;

  _groupedMode = 'Daily';
  groupedLoading: boolean;
  singleLoading: boolean;
  customGroupLoading: boolean;
  groupedNoData: boolean;
  nodesNoData: boolean;
  _singleMode = 'NODES';
  _customGroupMode = 'NODES';
  singleData:any;
  singleDataRows:string[];
  groupedChart: any;
  nodesLoading: boolean;
  nodes: Node[];
  columns: ListItem[];
  currentTab=0;

  set groupedStart(groupedStart:Date){
    this._groupedStart=groupedStart;
    this._groupedStart.setHours(0,0,0);
    this.refreshGroups();
  }
  get groupedStart(){
    return this._groupedStart;
  }
  set groupedEnd(groupedEnd:Date){
    this._groupedEnd=groupedEnd;
    this._groupedEnd.setHours(23,59,59);
    this.refreshGroups();
  }
  get groupedEnd(){
    return this._groupedEnd;
  }
  set groupedMode(groupedMode:string){
    this._groupedMode=groupedMode;
    this.refreshGroups();
  }
  get groupedMode(){
    return this._groupedMode;
  }
  set customGroupStart(customGroupStart:Date){
    this._customGroupStart=customGroupStart;
    this._customGroupStart.setHours(0,0,0);
    this.refreshCustomGroups();
  }
  get customGroupStart(){
    return this._customGroupStart;
  }
  set customGroupEnd(customGroupEnd:Date){
    this._customGroupEnd=customGroupEnd;
    this._customGroupEnd.setHours(23,59,59);
    this.refreshCustomGroups();
  }
  get customGroupEnd(){
    return this._customGroupEnd;
  }
  set customGroup(customGroup:string){
    this._customGroup=customGroup;
    this.refreshCustomGroups();
  }
  get customGroup(){
    return this._customGroup;
  }
  set customGroupMode(customGroupMode:string){
    this._customGroupMode=customGroupMode;
    this.refreshCustomGroups();
  }
  get customGroupMode(){
    return this._customGroupMode;
  }
  set customUnfold(customUnfold:string){
    this._customUnfold=customUnfold;
    this.refreshCustomGroups();
  }
  get customUnfold(){
    return this._customUnfold;
  }
  set singleStart(singleStart:Date){
    this._singleStart=singleStart;
    this._singleStart.setHours(0,0,0);
    this.refreshSingle();
  }
  get singleStart(){
    return this._singleStart;
  }
  set singleEnd(singleEnd:Date){
    this._singleEnd=singleEnd;
    this._singleEnd.setHours(23,59,59);
    this.refreshSingle();
  }
  get singleEnd(){
    return this._singleEnd;
  }
  set singleMode(singleMode:string){
    this._singleMode=singleMode;
    this.refreshSingle();
  }
  get singleMode(){
    return this._singleMode;
  }
  set nodesStart(nodesStart:Date){
    this._nodesStart=nodesStart;
    this._nodesStart.setHours(0,0,0);
    this.refreshNodes();
  }
  get nodesStart(){
    return this._nodesStart;
  }
  set nodesEnd(nodesEnd:Date){
    this._nodesEnd=nodesEnd;
    this._nodesEnd.setHours(23,59,59);
    this.refreshNodes();
  }
  get nodesEnd(){
    return this._nodesEnd;
  }
    constructor(
        private admin : RestAdminService,
        private statistics : RestStatisticsService,
        private translate : TranslateService,
        private config : ConfigurationService,
      ) {
      this.columns=[
          new ListItem('NODE',RestConstants.CM_NAME),
          new ListItem('NODE','counts.VIEW_MATERIAL'),
          new ListItem('NODE','counts.VIEW_MATERIAL_EMBEDDED'),
          new ListItem('NODE','counts.DOWNLOAD_MATERIAL'),
      ];
      this.groupedStart = new Date(new Date().getTime()-AdminStatisticsComponent.DEFAULT_OFFSET);
      this.groupedEnd = new Date();
      this.singleStart = new Date(new Date().getTime()-AdminStatisticsComponent.DEFAULT_OFFSET_SINGLE);
      this.singleEnd = new Date();
      this.customGroupStart = new Date(new Date().getTime()-AdminStatisticsComponent.DEFAULT_OFFSET);
      this.customGroupEnd = new Date();
      this.nodesStart = new Date(new Date().getTime()-AdminStatisticsComponent.DEFAULT_OFFSET);
      this.nodesEnd = new Date();

      // e.g. ['school']
      this.config.get('admin.statistics.groups',[]).subscribe((v)=>{
        this.additionalGroups=v;
        this.customGroups=['authority_organization','authority_mediacenter'].concat(v);
        if(this.customGroups.length)
          this.customGroup=this.customGroups[0];
      });
     this.refresh();
    }
  refresh(){
    this.refreshGroups();
    this.refreshNodes();
    this.refreshSingle();
    this.refreshCustomGroups();
  }

  private refreshGroups() {
    this.groupedLoading=true;
    this.statistics.getStatisticsNode(this._groupedStart,new Date(this._groupedEnd.getTime()+AdminStatisticsComponent.DAY_OFFSET),this._groupedMode,this.getMediacenter()).subscribe((dataNode)=> {
      if (this._groupedMode != 'None') {
        this.statistics.getStatisticsUser(this._groupedStart,new Date(this._groupedEnd.getTime()+AdminStatisticsComponent.DAY_OFFSET), this._groupedMode,this.getMediacenter()).subscribe((dataUser) => {
          this.processGroupData(dataNode,dataUser);
        });
      } else {
        this.processGroupData(dataNode,null);
      }
    });
  }

  getMediacenter(): string {
    return this._mediacenter ? this._mediacenter.authorityName : '';
  }

  processGroupData(dataNode : NodeStatistics[],dataUser : Statistics[]){
    this.groupedLoading=false;
    if(!dataNode.length){
      this.groupedNoData=true;
      return;
    }
    this.groupedNoData=false;
    UIHelper.waitForComponent(this,'groupedChartRef').subscribe(()=> {
      let canvas: any = this.groupedChartRef.nativeElement;
      let ctx = canvas.getContext('2d');
      if (this.groupedChart)
        this.groupedChart.destroy();
      this.groupedChart = this.initGroupedChart(dataNode, dataUser, ctx);
      console.log(this.groupedChart);
    });
  }

  private initGroupedChart(dataNode: NodeStatistics[],dataUser: Statistics[], ctx:any) {
    let max=dataNode.map((stat)=>
        Math.max(
            stat.counts['VIEW_MATERIAL'] || 0,
            stat.counts['VIEW_MATERIAL_EMBEDDED'] || 0,
            stat.counts['DOWNLOAD_MATERIAL'] || 0)).
        reduce((a,b)=>Math.max(a,b));
    max=Math.max(max,dataUser.map((stat)=>stat.counts['LOGIN_USER_SESSION'] || 0).reduce((a,b,)=>Math.max(a,b)));
    console.log(dataNode);
    console.log(max);
    let chartGroupedData = {
      labels: dataNode.map((stat) => stat.date),
      datasets: [{
          label: this.translate.instant('ADMIN.STATISTICS.VIEWS'),
          yAxisID: 'y-axis-view',
          backgroundColor: 'rgb(30,52,192)',
          data: dataNode.map((stat) => stat.counts['VIEW_MATERIAL'] ? stat.counts['VIEW_MATERIAL'] : 0)
        }, {
          label: this.translate.instant('ADMIN.STATISTICS.VIEWS_EMBEDDED'),
          yAxisID: 'y-axis-view-embedded',
          backgroundColor: 'rgb(117,48,192)',
          data: dataNode.map((stat) => stat.counts['VIEW_MATERIAL_EMBEDDED'] ? stat.counts['VIEW_MATERIAL_EMBEDDED'] : 0)
        }, {
          label: this.translate.instant('ADMIN.STATISTICS.DOWNLOADS'),
          yAxisID: 'y-axis-download',
          backgroundColor: 'rgb(40,146,192)',
          data: dataNode.map((stat) => stat.counts['DOWNLOAD_MATERIAL'] ? stat.counts['DOWNLOAD_MATERIAL'] : 0)
      }],
    };
    let axes=[{
      type: 'linear',
      display: true,
      position: 'left',
      id: 'y-axis-view',
      ticks: {
        beginAtZero: true,
        max: max,
        min: 0
      }
    }, {
        type: 'linear',
        display: false,
        id: 'y-axis-view-embedded',
        ticks: {
          beginAtZero: true,
          max: max,
          min: 0
        }
      }, {
      type: 'linear',
      display: false,
      id: 'y-axis-download',
      ticks: {
        beginAtZero: true,
        max: max,
        min: 0
      }
    }];
    if(dataUser){
      chartGroupedData.datasets.push({
        label: this.translate.instant('ADMIN.STATISTICS.USER_LOGINS'),
        yAxisID: 'y-axis-user',
        backgroundColor: 'rgb(22,192,73)',
        data: dataUser.map((stat) => stat.counts['LOGIN_USER_SESSION'] ? stat.counts['LOGIN_USER_SESSION'] : 0)
      });
      axes.push({
        type: 'linear',
        display: false,
        id: 'y-axis-user',
        ticks: {
          beginAtZero: true,
          max: max,
          min: 0
        }
      });
    }

    Chart.defaults.global.defaultFontFamily = 'inherit';
    return new Chart(ctx, {
      type: "bar",
      data: chartGroupedData,
      options: {
        responsive: true,
        aspectRatio: 3,
        legend: {
          display: true
        },
        mode: 'index',
        scales: {
          yAxes: axes,
        }
      }
    });
  }

  private refreshNodes() {
    this.nodes=[];
    this.nodesLoading = true;
    this.statistics.getStatisticsNode(this._nodesStart, new Date(this._nodesEnd.getTime()+AdminStatisticsComponent.DAY_OFFSET),'Node',this.getMediacenter()).subscribe((data) => {
      this.nodesLoading = false;
      this.nodesNoData = data.length==0;
      this.nodes = data.map((stat)=>{
        (stat.node as any).counts=stat.counts;
        return stat.node
      });
    });
  }
  openNode(entry:any){
    this.onOpenNode.emit(entry.node);
  }

  private refreshSingle() {
    this.singleDataRows=null;
    this.singleLoading=true;
    if(this._singleMode=='NODES'){
      this.singleDataRows=["date","action","node","authority","authority_organization","authority_mediacenter"].concat(this.additionalGroups || []);
      this.statistics.getStatisticsNode(this._singleStart,new Date(this._singleEnd.getTime()+AdminStatisticsComponent.DAY_OFFSET),'None',this.getMediacenter(),this.additionalGroups).subscribe((result)=>{
        this.singleData=result.map((entry)=> {
          return {"action": Object.keys(entry.counts)[0], "date": entry.date, "node": RestHelper.getName(entry.node), "authority":entry.authority, "entry": entry}
        });
        this.singleLoading=false;
      });
    }
    if(this._singleMode=='USERS'){
      this.singleDataRows=["date","action","authority","authority_organization","authority_mediacenter"].concat(this.additionalGroups || []);
      this.statistics.getStatisticsUser(this._singleStart,new Date(this._singleEnd.getTime()+AdminStatisticsComponent.DAY_OFFSET),'None',this.getMediacenter(),this.additionalGroups).subscribe((result)=>{
        this.singleData=result.map((entry)=> {
          return {"action": Object.keys(entry.counts)[0], "date": entry.date, "authority":entry.authority, "entry": entry}
        });
        this.singleLoading=false;
      });
    }
  }

  private refreshCustomGroups() {
    if(!this.customGroups)
      return;
    this.customGroupData=null;
    this.customGroupLoading=true;
    this.customGroupRows=[];
    let handleResult=(result:Statistics[])=>{
      this.customGroupRows=["action"].concat(this.customGroup).concat("count");
      if(this.customUnfold){
        // add all found values as a matrix
        let set=Array.from(new Set( result.map((entry)=>Object.keys(entry.groups[this.customUnfold])).
            reduce((a,b)=>a.concat(b)).
            filter((a)=>a!="")
        ));
        // container for storing the display (transformed authorities names) data for the table view
        this.customGroupLabels=[];
        if(this.customUnfold=='authority_organization' || this.customUnfold=='authority_mediacenter'){
          // transform the value for the horizontal list data if it's org/group
          set = set.map((key)=>{
            let authority=result.map((entry)=>((this.customUnfold=='authority_organization' ? entry.authority.organization : entry.authority.mediacenter as any[])))
            .reduce((a,b)=>a.concat(b))
            .filter((a)=>a.authorityName==key);
            if(authority.length)
              this.customGroupLabels[key]=new AuthorityNamePipe(this.translate).transform(authority[0],null);
            return key;
          });
        }
        this.customGroupRows=this.customGroupRows.concat(set);
      }
      if(result.length) {
        this.customGroupData = result.map((entry) => {
          let result = [];
          for (let key in entry.counts) {
            let displayValue=entry.fields[this.customGroup];
            // transform the value for the vertical list data if it's org/group
            if(this.customGroup=='authority_organization' || this.customGroup=='authority_mediacenter'){
              let obj=(((this.customGroup=='authority_organization' ? entry.authority.organization : entry.authority.mediacenter) as any));
              if(obj) {
                displayValue = obj.map((group: any) => {
                  return new AuthorityNamePipe(this.translate).transform(group, null);
                }).join(" ");
              }
              else{
                displayValue='';
              }

            }
            result.push({"entry": entry,"displayValue": displayValue, "count": entry.counts[key], "action": key});
          }
          return result;
        }).reduce((a, b) => a.concat(b));
        console.log(this.customGroupData);
      }
      this.customGroupLoading=false;
    };
    if(this._customGroupMode=='NODES'){
      this.statistics.getStatisticsNode(this._customGroupStart,new Date(this._customGroupEnd.getTime()+AdminStatisticsComponent.DAY_OFFSET),'None',this.getMediacenter(),this.customUnfold ? [this.customUnfold] : null,[this.customGroup]).subscribe((result)=>{
        handleResult(result);
      });
    }
    if(this._customGroupMode=='USERS'){
      this.statistics.getStatisticsUser(this._customGroupStart,new Date(this._customGroupEnd.getTime()+AdminStatisticsComponent.DAY_OFFSET),'None',this.getMediacenter(),this.customUnfold ? [this.customUnfold] : null,[this.customGroup]).subscribe((result)=>{
        handleResult(result);
      });
    }
  }

  getGroupKey(element: any, key: string) {
    return element.entry.groups[key] ? Object.keys(element.entry.groups[key])[0] : null
  }
}
