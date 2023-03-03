import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateService } from '@ngx-translate/core';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';
import { SearchPageService } from './search-page.service';

const translateServiceStub = {} as TranslateService;

describe('SearchPageService', () => {
    let service: SearchPageService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule, EduSharingApiModule.forRoot(), RouterTestingModule],
            providers: [
                SearchPageService,
                {
                    provide: TranslateService,
                    useValue: translateServiceStub,
                },
            ],
        });
        service = TestBed.inject(SearchPageService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
