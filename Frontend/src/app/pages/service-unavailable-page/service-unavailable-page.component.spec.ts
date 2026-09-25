import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServiceUnavailablePageComponent } from './service-unavailable-page.component';

describe('ServiceUnavailablePageComponent', () => {
    let fixture: ComponentFixture<ServiceUnavailablePageComponent>;
    let component: ServiceUnavailablePageComponent;
    let navigate: jasmine.Spy;

    beforeEach(() => {
        TestBed.configureTestingModule({ imports: [ServiceUnavailablePageComponent] });
        fixture = TestBed.createComponent(ServiceUnavailablePageComponent);
        component = fixture.componentInstance;
        navigate = spyOn(component, 'navigate');
        fixture.detectChanges();
    });

    afterEach(() => fixture.destroy());

    it('renders without any service of the app', () => {
        const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
        expect(text).toContain(component.texts.title);
        expect(text).toContain(component.texts.retry);
    });

    it('returns once rest/_about answers', async () => {
        const fetchSpy = spyOn(window, 'fetch').and.resolveTo(new Response('{}', { status: 200 }));
        await component.retry();
        expect((fetchSpy.calls.mostRecent().args[0] as URL).pathname).toMatch(/rest\/_about$/);
        expect(navigate).toHaveBeenCalledOnceWith(component.target());
    });

    it('stays while the backend answers with an error or not at all', async () => {
        spyOn(window, 'fetch').and.returnValues(
            Promise.resolve(new Response('', { status: 503 })),
            Promise.reject(new TypeError('Failed to fetch')),
        );
        await component.retry();
        await component.retry();
        expect(navigate).not.toHaveBeenCalled();
        expect(component.checking()).toBeFalse();
    });
});
