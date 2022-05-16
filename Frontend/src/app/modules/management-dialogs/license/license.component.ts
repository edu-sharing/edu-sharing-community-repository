import {forkJoin as observableForkJoin, Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, RestConnectorService, RestIamService} from '../../../core-module/core.module';
import {Toast} from '../../../core-ui-module/toast';
import {RestNodeService} from '../../../core-module/core.module';
import {RestConstants} from '../../../core-module/core.module';
import {NodeWrapper, Node, NodePermissions, LocalPermissionsResult, Permission} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {NodeHelperService} from '../../../core-ui-module/node-helper.service';
import {ConfigurationService} from '../../../core-module/core.module';
import {RestHelper} from '../../../core-module/core.module';
import {VCard} from '../../../core-module/ui/VCard';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {UIService} from '../../../core-module/core.module';
import {Helper} from '../../../core-module/rest/helper';
import { Values } from 'dist/edu-sharing-api/lib/api/models';
import { UserPresentableError } from '../../../features/mds/mds-editor/mds-editor-common.service';
import { MdsEditorInstanceService } from '../../../features/mds/mds-editor/mds-editor-instance.service';
import { ViewInstanceService } from '../../../features/mds/mds-editor/mds-editor-view/view-instance.service';
import { MdsEditorWidgetAuthorComponent } from '../../../features/mds/mds-editor/widgets/mds-editor-widget-author/mds-editor-widget-author.component';

@Component({
    selector: 'es-workspace-license',
    templateUrl: 'license.component.html',
    styleUrls: ['license.component.scss'],
    providers: [
        MdsEditorInstanceService,
        ViewInstanceService
    ],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
        trigger('dialog', UIAnimation.switchDialog())
    ]
})
export class WorkspaceLicenseComponent  {
    set primaryType(primaryType:string) {
        this._primaryType=primaryType;
        this.updateButtons();
    }
    get primaryType() {
        return this._primaryType;
    }
    public set type(type:string) {
        if(type=='CC_0' || type=='PDM') {
            this.cc0Type=type;
            type='CC_0';
        }
        if(type=='CC_BY') {
            this.ccCommercial='';
            this.ccShare='';
        }
        if(type=='CC_BY_SA') {
            type='CC_BY';
            this.ccShare='SA';
        }
        if(type.startsWith('COPYRIGHT')) {
            this.copyrightType=type;
            type='COPYRIGHT';
        }
        this.primaryType=type;
    }
    public get type() {
        return this.getLicenseProperty();
    }
    public get getccCountries() {
        return this._ccCountries;
    }
    public set oerMode(oerMode:boolean) {
        this._oerMode=oerMode;
        this.showCcAuthor=false;
        if(oerMode) {
            if(this.isOerLicense()) {
                return;
            } else {
                this.type = 'NONE';
            }
        }
    }
    public get oerMode() {
        return this._oerMode;
    }
    @Input() set properties(properties : any) {
        this.loadConfig();
        this._properties = properties;
        this.readLicense();
        this.mdsEditorInstanceService.initWithNodes([({
            properties
        } as any)], { refetch: false });
        this.loading=false;
        this.updateButtons();
    }
    @Input() set nodes(nodesIn : Node[]) {
        this.loading = true;
        this.loadNodes(nodesIn).subscribe(async (nodes)=> {
            try {
                await this.mdsEditorInstanceService.initWithNodes(nodes);
            } catch(e) {
                if(e instanceof UserPresentableError || e.message) {
                    this.toast.error(null, e.message);
                } else {
                    this.toast.error(e);
                }
                this.cancel();
                return;
            }
            this.loadConfig();
            this.checkAllowRelease();
            this.readLicense();
            this.loading=false;
            this.updateButtons();
            this.releaseMulti=null;
            let i=0;
            for(const node of nodes) {
                i++;
                this.nodeApi.getNodePermissions(node.ref.id).subscribe((permissions: NodePermissions) => {
                    this.permissions = permissions.permissions.localPermissions;
                    this.readPermissions(i==this.getNodes()?.length);
                });
            }
        }, error => {
            this.toast.error(error);
            this.cancel();
        });
    }
    constructor(
        private connector : RestConnectorService,
        private translate : TranslateService,
        private config : ConfigurationService,
        private nodeHelper: NodeHelperService,
        private mdsEditorInstanceService : MdsEditorInstanceService,
        private ui : UIService,
        private iamApi : RestIamService,
        private toast : Toast,
        private nodeApi : RestNodeService) {
        this.translateLicenceCountries(this.constantCountries);
        this.updateButtons();
        this.iamApi.getCurrentUserAsync().then(() => {});
    }
    @ViewChild('selectLicense') selectLicense : ElementRef;
    @ViewChild('author') author : MdsEditorWidgetAuthorComponent;

    /**
     * priority, useful if the dialog seems not to be in the foreground
     * Values greater 0 will raise the z-index
     * Default is 1 for mds
     */
    @Input() priority = 1;
    @Input() embedded = false;
    _primaryType='';
    _properties: any;
    buttons: DialogButton[];
    public ccShare='';
    ccCommercial='';
    ccTitleOfWork='';
    ccSourceUrl='';
    ccVersion='4.0';
    ccCountry='';
    cc0Type='CC_0';
    ccProfileUrl='';
    copyrightType='COPYRIGHT_FREE';
    eduType='P_NR';
    rightsDescription='';
    private showCcAuthor=false;
    private contact=true;
    private contactIndeterminate=false;
    private release=false;
    private releaseIndeterminate=false;
    private eduDownload=true;
    _ccCountries: Array<{ key: string, name: string }> = [];

    private _oerMode=true;
    private constantCountries = [
        'AE','AL','AR','AT','AU','BA','BG','BH','BO','BR','BY','CA','CH','CL','CN','CO','CR','CY','CZ','DE','DK',
        'DO','DZ','EC','EE','EG','ES','FI','FR','GB','GR','GT','HK','HN','HR','HU','ID','IE','IL','IN','IQ','IS',
        'IT','JO','JP','KR','KW','LB','LT','LU','LV','LY','MA','ME','MK','MT','MX','MY','NI','NL','NO','NZ','OM',
        'PA','PE','PH','PL','PR','PT','PY','QA','RO','RS','RU','SA','SD','SE','SG','SI','SK','SY','TH','TN','TR','TW',
        'UA','US','UY','VE','VN','YE','ZA'
    ];
    public ALL_LICENSE_TYPES=['NONE','CC_0','CC_BY','SCHULFUNK','UNTERRICHTS_UND_LEHRMEDIEN','COPYRIGHT','CUSTOM'];
    public licenseMainTypes:string[];
    private permissions: LocalPermissionsResult;
    public loading=true;
    private allowedLicenses: string[];
    private releaseMulti: string;
    private allowRelease = true;
    userAuthor = false;
    @Output() onCancel=new EventEmitter();
    @Output() onLoading=new EventEmitter();
    @Output() onDone =new EventEmitter<Node[] | Values>();
    @Output() openContributor=new EventEmitter();

    public isAllowedLicense(license:string) {
        return this.allowedLicenses==null || this.allowedLicenses.indexOf(license)!=-1;
    }
    public isOerLicense() {
        return this.getLicenseProperty()=='CC_0' || this.getLicenseProperty()=='PDM'
            || this.getLicenseProperty()=='CC_BY' || this.getLicenseProperty()=='CC_BY_SA';
    }
    public loadNodes(nodes:Node[]) {
        return observableForkJoin(
            nodes.map((n) =>
                this.nodeApi.getNodeMetadata(n.ref.id, [RestConstants.ALL]).pipe(map((n2) => n2.node))
            )
        );
    }
    private loadConfig() {
        this.config.get('allowedLicenses').subscribe((data: string[]) => {
            if (!data) {
                this.licenseMainTypes = this.ALL_LICENSE_TYPES;
                this.allowedLicenses = null;
            }
            else {
                this.licenseMainTypes = [];
                this.allowedLicenses = data;
                for (const entry of data) {
                    if (entry.startsWith('CC_BY')) {
                        if (this.licenseMainTypes.indexOf('CC_BY') == -1)
                            this.licenseMainTypes.push('CC_BY');
                    }
                    else if (entry == 'CC_0' || entry == 'PDM') {
                        if (this.licenseMainTypes.indexOf('CC_0') == -1)
                            this.licenseMainTypes.push('CC_0');
                    }
                    else if (entry.startsWith('COPYRIGHT')) {
                        this.licenseMainTypes.push('COPYRIGHT');
                        if (data.indexOf(this.copyrightType) == -1)
                            this.copyrightType = entry;
                    }
                    else if (this.ALL_LICENSE_TYPES.indexOf(entry) != -1) {
                        this.licenseMainTypes.push(entry);
                    }
                }
            }
            for(const license of this.config.instant('customLicenses',[])) {
                this.licenseMainTypes.splice(license.position>=0 ? license.position : this.licenseMainTypes.length-license.position,0,license.id);
            }
        });
    }
    public cancel() {
        this.onCancel.emit();
    }

    public async saveLicense(callback: Function = null) {
        if(this.type === 'CUSTOM' && !this.rightsDescription.trim()) {
            this.toast.error(null, 'LICENSE.DESCRIPTION_REQUIRED');
            return;
        }
        if (this._properties) {
            this.onDone.emit(await this.getProperties(this._properties));
            return;
        }
        if (!this.getLicenseProperty() && this.release) {
            // this.toast.error(null,'WORKSPACE.LICENSE.RELEASE_WITHOUT_LICENSE');
            // return;
        }
        let prop: Values = {};

        prop = await this.getProperties(prop);
        let i = 0;
        this.onLoading.emit(true);
        const updatedNodes: Node[] = [];
        for (const node of this.getNodes()) {
            node.properties = prop;
            i++;
            this.nodeApi.editNodeMetadataNewVersion(node.ref.id, RestConstants.COMMENT_LICENSE_UPDATE, prop).subscribe((result) => {
                updatedNodes.push(result.node);
                this.savePermissions(node);
                if (updatedNodes.length === this.getNodes().length) {
                    this.toast.toast('WORKSPACE.TOAST.LICENSE_UPDATED');
                    this.onLoading.emit(false);
                    this.onDone.emit(updatedNodes);
                    if (callback) {
                        callback();
                    }
                }
            }, (error: any) => {
                this.onLoading.emit(false);
                this.toast.error(error);
            });
        }
    }
    getNodes() {
        return this.mdsEditorInstanceService.nodes$.value;
    }
    private getValueForAll(prop:string,fallbackNotIdentical:any='',fallbackIsEmpty=fallbackNotIdentical,asArray=false) {
        if(this._properties) {
            return this._properties[prop] ? this._properties[prop][0] : fallbackIsEmpty;
        }
        if(this.getNodes()) {
            return this.nodeHelper.getValueForAll(this.getNodes(), prop, fallbackNotIdentical, fallbackIsEmpty, asArray);
        } else{
            return fallbackIsEmpty;
        }
    }
    private readLicense() {
        let license=this.getValueForAll(RestConstants.CCM_PROP_LICENSE,'MULTI','NONE');
        if(!license)
            license='NONE';
        this.type=license;
        if(license.startsWith('CC_BY')) {
            this.type='CC_BY';
            if(license.indexOf('SA')!=-1)
                this.ccShare='SA';
            if(license.indexOf('ND')!=-1)
                this.ccShare='ND';
            if(license.indexOf('NC')!=-1)
                this.ccCommercial='NC';

            this.ccTitleOfWork=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK);
            this.ccSourceUrl=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_SOURCE_URL);
            this.ccProfileUrl=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_PROFILE_URL);
            this.ccVersion=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_CC_VERSION,this.ccVersion);
            this.ccCountry=this.getValueForAll(RestConstants.CCM_PROP_LICENSE_CC_LOCALE);
        }
        if(license=='CC_0') {
            this.type='CC_0';
        }
        if(license=='PDM') {
            this.type='PDM';
        }
        if(license.startsWith('COPYRIGHT')) {
            this.type='COPYRIGHT';
            this.copyrightType=license;
        }
        if(license=='SCHULFUNK') {
            this.type=license;
        }
        if(license.startsWith('EDU')) {
            this.type='EDU';
            if(license.indexOf('P_NR')!=-1)
                this.eduType='P_NR';
            if(license.indexOf('NC')!=-1)
                this.eduType='NC';
            this.eduDownload=license.indexOf('ND')==-1;
        }
        if(license=='CUSTOM')
            this.type=license;

        this.rightsDescription=this.getValueForAll(RestConstants.LOM_PROP_RIGHTS_DESCRIPTION);
        const contactState=this.getValueForAll(RestConstants.CCM_PROP_QUESTIONSALLOWED,'multi','true');
        this.contact=contactState=='true' || contactState==true;
        this.oerMode=this.isOerLicense() || this.primaryType=='NONE';
        UIHelper.invalidateMaterializeTextarea('authorFreetext');
        UIHelper.invalidateMaterializeTextarea('licenseRights');
        this.contactIndeterminate=contactState=='multi';
    }

    getLicenseProperty() {
        let name=this.primaryType;
        if(this.primaryType=='NONE')
            return '';
        if(this.primaryType=='CC_BY') {
            if(this.ccCommercial)
                name+='_'+this.ccCommercial;
            if(this.ccShare)
                name+='_'+this.ccShare;
            return name;
        }
        if(this.primaryType=='CC_0') {
            return this.cc0Type;
        }
        if(this.primaryType=='COPYRIGHT') {
            return this.copyrightType;
        }
        if(this.primaryType=='EDU') {
            name+='_'+this.eduType;
            if(!this.eduDownload)
                name+='_ND';
        }

        return name;
    }
    getLicenseName() {
        return this.nodeHelper.getLicenseNameByString(this.getLicenseProperty());
    }
    getLicenseUrl() {
        return this.nodeHelper.getLicenseUrlByString(this.getLicenseProperty(),this.ccVersion);
    }
    getLicenseUrlVersion(type:string) {
        return this.nodeHelper.getLicenseUrlByString(type,this.ccVersion);
    }
    getLicenseIcon() {
        return this.nodeHelper.getLicenseIconByString(this.getLicenseProperty());
    }
    private savePermissions(node:Node) {
        if(this.releaseIndeterminate) {
            return;
        }
        let add=true;
        let index=0;
        for(const perm of this.permissions.permissions) {
            if(perm.authority.authorityName==RestConstants.AUTHORITY_EVERYONE) {
                add=false;
                if(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH)==-1 && this.release) {
                    perm.permissions.push(RestConstants.ACCESS_CC_PUBLISH);
                } if(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER)==-1 && this.release) {
                    perm.permissions.push(RestConstants.ACCESS_CONSUMER);
                }
                /*if(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH)!=-1 && !this.release){
                  perm.permissions.splice(perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH),1);
                }
                if(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER)!=-1 && !this.release){
                  perm.permissions.splice(perm.permissions.indexOf(RestConstants.ACCESS_CONSUMER),1);
                }
                */
                break;
            }
            index++;
        }
        // remove all_authorities
        if(!add && !this.release) {
            this.permissions.permissions.splice(index,1);
        }
        // add all_authorities
        if(add && this.release) {
            const perm=RestHelper.getAllAuthoritiesPermission();
            perm.permissions=[RestConstants.ACCESS_CC_PUBLISH,RestConstants.ACCESS_CONSUMER];
            this.permissions.permissions.push(perm);
        }
        const permissions=RestHelper.copyAndCleanPermissions(this.permissions.permissions,this.permissions.inherited);
        this.nodeApi.setNodePermissions(node.ref.id,permissions,false,'',false).subscribe(()=> {
        },(error:any)=>this.toast.error(error));
    }
    private readPermissions(last:boolean) {
        this.release=false;
        if(this)
            for(const perm of this.permissions.permissions) {
                if(perm.authority.authorityType==RestConstants.AUTHORITY_TYPE_EVERYONE && perm.permissions.indexOf(RestConstants.ACCESS_CC_PUBLISH)!=-1) {
                    if(this.releaseMulti!=null && this.releaseMulti!='true')
                        this.releaseMulti='multi';
                    else
                        this.releaseMulti='true';
                    if(last)
                        this.setPermissionState();
                    return;
                }
            }
        if(this.releaseMulti!=null && this.releaseMulti!='false')
            this.releaseMulti='multi';
        else
            this.releaseMulti='false';
        if(last)
            this.setPermissionState();
    }

    private setPermissionState() {
        if(this.releaseMulti=='true')
            this.release=true;
        if(this.releaseMulti==null || this.releaseMulti=='false')
            this.release=false;

        if(this.releaseMulti=='multi') {
            this.releaseIndeterminate=true;
        }
    }

    private checkAllowRelease() {
        this.connector.hasToolPermission(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES).subscribe((data:boolean)=> {
            if(!this.getNodes()){
                return;
            }
            if(!data) {
                this.allowRelease = false;
                return;
            }
            for(const node of this.getNodes()) {
                if(node.access.indexOf(RestConstants.ACCESS_CHANGE_PERMISSIONS)==-1) {
                    this.allowRelease=false;
                    return;
                }
            }
        });
    }
    setCCBy() {
        this.type='CC_BY';
        this.ccShare='';
        this.ccCommercial='';
    }
    setCC0() {
        this.type='CC_0';
        this.cc0Type='CC_0';
    }

    async getProperties(prop = this._properties) {
        prop[RestConstants.CCM_PROP_LICENSE] = [this.getLicenseProperty()];
        if (!this.contactIndeterminate)
            prop[RestConstants.CCM_PROP_QUESTIONSALLOWED] = [this.contact];
        if (this.isCCAttributableLicense()) {
            prop[RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK] = null;
            prop[RestConstants.CCM_PROP_LICENSE_SOURCE_URL] = null;
            prop[RestConstants.CCM_PROP_LICENSE_PROFILE_URL] = null;
            prop[RestConstants.CCM_PROP_LICENSE_CC_VERSION] = null;
            prop[RestConstants.CCM_PROP_LICENSE_CC_LOCALE] = null;
            if (this.ccTitleOfWork) {
                prop[RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK] = [this.ccTitleOfWork];
            }
            if (this.ccSourceUrl) {
                prop[RestConstants.CCM_PROP_LICENSE_SOURCE_URL] = [this.ccSourceUrl];
            }
            if (this.ccProfileUrl) {
                prop[RestConstants.CCM_PROP_LICENSE_PROFILE_URL] = [this.ccProfileUrl];
            }
            if (this.ccVersion) {
                prop[RestConstants.CCM_PROP_LICENSE_CC_VERSION] = [this.ccVersion];
            }
            if (this.ccCountry) {
                prop[RestConstants.CCM_PROP_LICENSE_CC_LOCALE] = [this.ccCountry];
            }
        }
        prop = await this.author.getValues(prop, this.getNodes()?.length === 1 ? this.getNodes()[0] : null);


        if (this.type == 'CUSTOM') {
            prop[RestConstants.LOM_PROP_RIGHTS_DESCRIPTION] = [this.rightsDescription];
        }
        return prop;
    }

    openContributorDialog() {
        const nodes=this.getNodes();
        this.saveLicense(()=> {
            this.openContributor.emit(nodes);
        });
    }

    changeRelease(release:boolean) {
        if(release) {
            if(this.config.instant('publishingNotice',false)) {
                const cancel=()=> {
                    this.release=false;
                    this.toast.closeModalDialog();
                };
                this.toast.showModalDialog('WORKSPACE.SHARE.PUBLISHING_WARNING_TITLE',
                    'WORKSPACE.SHARE.PUBLISHING_WARNING_MESSAGE',
                    DialogButton.getYesNo(cancel, ()=> {
                        this.release=true;
                        this.toast.closeModalDialog();
                    }),true,cancel);


                return;
            }
        }
    }

    private updateButtons() {
        const save=new DialogButton('SAVE',DialogButton.TYPE_PRIMARY,()=>this.saveLicense());
        save.disabled=this.loading || this.type=='MULTI';
        this.buttons=[
            new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>this.cancel()),
            save
        ];
    }

    isCCAttributableLicense() {
        return this.getLicenseProperty() && this.getLicenseProperty().startsWith('CC_BY');
    }

    /**
     * Get all the key from countries and return the array with key and name (Translated)
     * @param {string[]} countries array with all Countries Key
     */
    translateLicenceCountries(countries: string[]) {
        this._ccCountries=[];
        countries.forEach(country => {
            this._ccCountries.push({ key: country, name: this.translate.instant('COUNTRY_CODE.' + country) })
        });
        this._ccCountries.sort((a, b) => this.sortCountries({ a: a.name, b: b.name }));
    }

    /**
     * Function wich compare 2 string and return one of those numbers -1,0,1
     *
     *   -1 if a<b
     *    1 if a>b
     *    0 if a=b
     *
     * @param {string} a first string
     * @param {string} b second string
     * @returns {number}   -1 | 0 | 1
     */
    private sortCountries({ a, b }: { a: string; b: string; }):number {
        if (a.toLowerCase() < b.toLowerCase()) return -1;
        if (a.toLowerCase() > b.toLowerCase()) return 1;
        return 0;
    }
}
